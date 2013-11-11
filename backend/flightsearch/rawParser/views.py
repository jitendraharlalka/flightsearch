from django.shortcuts import render
from django.http import HttpResponse

# Create your views here.
def index(request):
	return HttpResponse('Hello, world. I will send back what you send me.')

def origin(request, city):
	return HttpResponse(city)

def destination(request, city):
	return HttpResponse(city)

def departureDate(request, date):
	return HttpResponse(date)

def departureTime(request, time):
	return HttpResponse(time)
