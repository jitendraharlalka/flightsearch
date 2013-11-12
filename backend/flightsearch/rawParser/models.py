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


