from django.views.decorators.csrf import csrf_exempt
from datetime import datetime,timedelta
from django.conf import settings
from django.shortcuts import render
from django.http import HttpResponse
import parsedatetime as pdt
from parsedatetime import Constants
from pytz import timezone
from rawParser.models import Airports,Flights
import utils,logging,json,ast
from django.core import serializers

# Create your views here.
TZ = timezone(getattr(settings, "TIME_ZONE", "UTC"))
broadTimes=['morning','noon','evening','night','midnight']
meridian=[' a.m.',' p.m.',' am',' pm']
logger = logging.getLogger(__name__)


def index(request):
	return HttpResponse('Hello, world. This is s a static text.')

def tagger(request,text):
	tags=utils.nerTag(text)
	tags=ast.literal_eval(tags)
	tags=uniteCityTokens(tags)
	tags=processDateTokens(tags)
	tags=processTimeTokens(tags)
	#res=search(tags)
	#return HttpResponse(str(res))
	logger.debug(tags)
	return HttpResponse(str(tags))

@csrf_exempt
def search(request):
	#logger.debug(dir(request))
	logger.debug(request)
	logger.debug('BODY')
	logger.debug(request.body)
	logger.debug('BODY TERMINATED')
	if request.method== 'POST':
		logger.debug(request.POST)
		fields=ast.literal_eval(request.body)	
	logger.debug(fields)
	flights=Flights.objects.filter()
	for key in fields:
		logger.debug(key)
		fl=[]
		for val in fields[key]:
			if key=='TIME':
				splitVals=val.split('-')
				if len(splitVals)==2:
					logger.debug(splitVals)
					tmp=Flights.objects.filter(starttime__gt=splitVals[0],starttime__lt=splitVals[1])
					logger.debug('For the evening number of flights = '+str(len(tmp)))
					fl=list(set(fl) | set(tmp))		
				else:
					statedTime=datetime.strptime(val,'%H:%M')
					lwLimit=statedTime-timedelta(hours=1)
					upLimit=statedTime+timedelta(hours=1)
					lower=lwLimit.strftime('%H:%M')
					upper=upLimit.strftime('%H:%M')
					tmp=Flights.objects.filter(starttime__gt=lower,starttime__lt=upper)
					fl=list(set(fl) | set(tmp))
					logger.debug('Returning for time: '+str(len(fl)))
			elif key=='DATE':
				originalForm=datetime.strptime(val,'%A, %d %B %Y')
				dbForm=originalForm.strftime('%Y-%m-%d')
				tmp=Flights.objects.filter(departdate=dbForm)
				fl=list(set(fl) | set(tmp))
				logger.debug('Returning for date: '+str(len(fl)))
			elif key=='ORIGIN':
				tmp=Flights.objects.filter(origin__iexact=val)
				fl=list(set(fl) | set(tmp))
				logger.debug('Returning for origin: '+str(len(fl)))
			elif key=='DESTINATION':
				tmp=Flights.objects.filter(destination__iexact=val)
				fl=list(set(fl) | set(tmp))
				logger.debug('Returning for destination: '+str(len(fl)))
		flights=list(set(flights) & set(fl))
		logger.debug('Total returned flights = ' + str(len(flights)))
	flights=sorted(flights,key=lambda x:x.cost)
	json_result=serializers.serialize('json',flights)
	logger.debug('Returning flights')
	return HttpResponse(str(json_result))

def origin(request, incity):
	outcity=disambiguateCity(incity)
	return HttpResponse(outcity)

def destination(request, incity):
	outcity=disambiguateCity(incity)
	return HttpResponse(outcity)

def departureDate(request, dateTime):
	formatted=parse(dateTime)
	
	flag1=0
        normalized=dateTime.lower()
        for time in broadTimes:
                try:
                        idx=normalized.index(time)
                        flag1=1
                        break
                except ValueError:
                        pass 
                
        if flag1==1:
		format1=formatted-timedelta(hours=2)
		format2=formatted+timedelta(hours=5)
		date1=format1.strftime("%A, %d %B %Y")
		date2=format2.strftime("%A, %d %B %Y")
		time1=format1.strftime("%H:%M")
		time2=format2.strftime("%H:%M")
		if date1==date2:
			return HttpResponse(date1+"#"+time1+"#"+time2)
		else:
			return HttpResponse(date1+"#"+date2+"#"+time1+"#"+time2)
	flag2=0
	for mrdn in meridian:
		try:
			idx=normalized.index(mrdn)
			flag2=1
			break
		except ValueError:
			pass
	if flag2==1:
		date=formatted.strftime("%A, %d %B %Y")
		time=formatted.strftime("%H:%M")
		return HttpResponse(date+"#"+time)	

	date=formatted.strftime("%A, %d %B %Y")
	return HttpResponse(date)

#def departureTime(request, time):
	#return HttpResponse(parse(time).time())

def disambiguateCity(incity):
	airports=Airports.objects.filter(code__iexact=incity)
	outcity="-1"
	if len(airports)>0:
		outcity=airports[0].city
	else:
		airports=Airports.objects.filter(city__icontains=incity)
		if len(airports)>0:
			for ap in airports:
				if incity.lower() in map(lambda x:x.lower(), ap.city.split()):
					outcity=ap.city
					break
		else:
			airports=Airports.objects.filter(airport__icontains=incity)
			for ap in airports:
				if incity.lower() in map(lambda x:x.lower(), ap.airport.split()):
					outcity=ap.city
					break
	return outcity


def disambiguateCityTokens(values,component=""):
	airports=Airports.objects.filter()
	#component=""
	for token in values:
		if component=="" or component=="code":
			apTokens=Airports.objects.filter(code__iexact=token)
			if len(apTokens)>0:
				component="code"
				airports=list(set(airports) & set(apTokens))
			else:
				airports=disambiguateCityTokens(values,"codeExamined")
		elif component=="codeExamined" or component=="city":
			apTokens=Airports.objects.filter(city__icontains=token)
			if len(apTokens)>0:
				component="city"
				airports=list(set(airports) & set(apTokens))
			else:
				airports=disambiguateCityTokens(values,"cityExamined")
		elif component=="cityExamined" or component=="airport":
			apTokens=Airports.objects.filter(airport__icontains=token)
			if len(apTokens)>0:
				component="airport"
				airports=list(set(airports) & set(apTokens))
			else:
				airports=disambiguateCityTokens(values,"airportExamined")
		elif component=="airportExamined":
			airports="-1"
	
	return airports

def uniteCityTokens(tags):
	fieldKeys=['ORIGIN','DESTINATION']
	for key in tags:
		if key in fieldKeys:
			logger.debug(tags[key])
			airports=disambiguateCityTokens(tags[key])
			if airports=="-1":
				tags[key]="-1"
			else:
				tags[key]=[]			
				for ap in airports:
					tags[key].append(ap.code.encode('ascii','ignore'))
		if key=='LOCATION':
			apcodes=[]
			for i,unit in enumerate(tags[key]):
				logger.debug(unit)
				airports=disambiguateCityTokens(unit)
				if airports=="-1":
					apcodes.append("-1")
				else:
					for ap in airports:
						apcodes.append(ap.code.encode('ascii','ignore'))
			tags[key]=apcodes 
	return tags


def processDateTokens(tags):
	field=['DATE']
	for key in tags:
		if key in field:
			for i,val in enumerate(tags[key]):
				raw=" ".join(val)
				normDate=utils.processDate(raw)
				tags[key][i]=normDate
	return tags


def processTimeTokens(tags):
	field=['TIME']
	for key in tags:
		if key in field:
			for i,val in enumerate(tags[key]):
				raw=" ".join(val)
				normTime=utils.processTime(raw)
				tags[key][i]=normTime
	return tags

def parse(s):
	return TZ.localize(datetime(*tuple(pdt.Calendar(Constants()).parse(s)[0])[:7]))
