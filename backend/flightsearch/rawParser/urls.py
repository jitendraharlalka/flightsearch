from django.conf.urls import patterns, url

from rawParser import views

urlpatterns = patterns('',
    url(r'^$', views.index, name='index'),
    url(r'^origin/(?P<city>.+)/$',views.origin, name='origin'),
    url(r'^destination/(?P<city>.+)/$',views.destination, name='destination'),
    url(r'^departDate/(?P<date>.+)/$',views.departureDate, name='departDate'),
    url(r'^departTime/(?P<time>.+)/$',views.departureTime, name='departTime'),
)

