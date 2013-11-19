from datetime import datetime,timedelta
from django.conf import settings
from django.shortcuts import render
from django.http import HttpResponse
import parsedatetime as pdt
from parsedatetime import Constants
from pytz import timezone
from rawParser.models import Airports
import utils

# Create your views here.
TZ = timezone(getattr(settings, "TIME_ZONE", "UTC"))
broadTimes=['morning','noon','evening','night','midnight']
meridian=[' a.m.',' p.m.',' am',' pm']

def index(request):
	return HttpResponse('Hello, world. This is s a static text.')

def tagger(request,text):
	return HttpResponse(utils.nerTag(text))	

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


def parse(s):
	return TZ.localize(datetime(*tuple(pdt.Calendar(Constants()).parse(s)[0])[:7]))
