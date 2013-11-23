from datetime import datetime,timedelta
from django.conf import settings
from django.shortcuts import render
from django.http import HttpResponse
import parsedatetime as pdt
from parsedatetime import Constants
from pytz import timezone
from rawParser.models import Airports
import utils,logging,json,ast


# Create your views here.
TZ = timezone(getattr(settings, "TIME_ZONE", "UTC"))
broadTimes=['morning','noon','evening','night','midnight']
meridian=[' a.m.',' p.m.',' am',' pm']
logger = logging.getLogger(__name__)


def index(request):
	return HttpResponse('Hello, world. This is s a static text.')

def tagger(request,text):
	tags=utils.nerTag(text)
	#return HttpResponse(tags)
	logger.debug('Called ner tagger')
	tags=ast.literal_eval(tags)
	logger.debug(tags)
	tags=uniteCityTokens(tags)
	tags=processDateTokens(tags)
	tags=processTimeTokens(tags)
	logger.debug(tags)
	return HttpResponse(str(tags))

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
	logger.debug('Inside disambiguateCityTokens')
	#component=""
	for token in values:
		logger.debug('Token')
		logger.debug(token)
		if component=="" or component=="code":
			apTokens=Airports.objects.filter(code__iexact=token)
			if len(apTokens)>0:
				component="code"
				logger.debug(token+' '+str(apTokens))
				airports=list(set(airports) & set(apTokens))
			else:
                        	#if component=="code":
					#logger.debug('Code continue')
				airports=disambiguateCityTokens(values,"codeExamined")
					#break
				#else:
					#logger.debug('Finish Code')
					#component="codeFinished"
		elif component=="codeExamined" or component=="city":
			logger.debug('Inside City center')
			apTokens=Airports.objects.filter(city__icontains=token)
			if len(apTokens)>0:
				component="city"
				airports=list(set(airports) & set(apTokens))
			else:
				#if component=="city":
				airports=disambiguateCityTokens(values,"cityExamined")
					#break
				#else:
					#component="cityFinished"
		elif component=="cityExamined" or component=="airport":
			logger.debug('Inside Airport center')
			apTokens=Airports.objects.filter(airport__icontains=token)
			if len(apTokens)>0:
				component="airport"
				airports=list(set(airports) & set(apTokens))
			else:
				#if component=="airport":
				airports=disambiguateCityTokens(values,"airportExamined")
					#break
				#else:
					#component="airportFinished"
		elif component=="airportExamined":
			airports="-1"
	
	logger.debug('Returning '+str(len(airports))+' airportCount')
	return airports

def uniteCityTokens(tags):
	fieldKeys=['ORIGIN','DESTINATION']
	logger.debug('Uniting City tokens')
	for key in tags:
		logger.debug(key+' processin')
		if key in fieldKeys:
			logger.debug(tags[key])
			airports=disambiguateCityTokens(tags[key])
			if airports=="-1":
				tags[key]="-1"
			else:
				tags[key]=[]			
				for ap in airports:
					tags[key].append(ap.code)
		if key=='LOCATION':
			logger.debug(tags[key])
			apcodes=[]
			for i,unit in enumerate(tags[key]):
				logger.debug('unit')
				logger.debug(unit)
				airports=disambiguateCityTokens(unit)
				if airports=="-1":
					apcodes.append("-1")
				else:
					#tags[key][i]=[]
					for ap in airports:
						apcodes.append(ap.code)
						#tags[key][i].append(ap.code)
			tags[key]=apcodes 
	logger.debug(tags)
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
