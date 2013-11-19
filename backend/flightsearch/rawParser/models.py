from django.db import models

# Create your models here.
class flightSearch(models.Model):
	origin=models.CharField(max_length=200)
	destination=models.CharField(max_length=200)
	departDate=models.DateTimeField('Departure Date')

class Airports(models.Model):
    code = models.CharField(primary_key=True, max_length=5)
    airport = models.CharField(max_length=255, blank=True)
    city = models.CharField(max_length=50, blank=True)
    state = models.CharField(max_length=5, blank=True)
    class Meta:
        managed = True
        db_table = 'airports'

class Flights(models.Model):
    id = models.IntegerField(primary_key=True)
    origin = models.CharField(max_length=255)
    destination = models.CharField(max_length=255)
    starttime = models.TimeField(db_column='startTime') # Field name made lowercase.
    endtime = models.TimeField(db_column='endTime') # Field name made lowercase.
    departdate = models.DateField(db_column='departDate') # Field name made lowercase.
    airline = models.CharField(max_length=255)
    cost = models.IntegerField()
    stops = models.IntegerField(blank=True, null=True)
    class Meta:
        managed = True
        db_table = 'flights'

