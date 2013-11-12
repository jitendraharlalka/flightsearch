from django.conf.urls import patterns, url

from rawParser import views

urlpatterns = patterns('',
    url(r'^$', views.index, name='index'),
    url(r'^origin/(?P<incity>.+)/$',views.origin, name='origin'),
    url(r'^destination/(?P<incity>.+)/$',views.destination, name='destination'),
    url(r'^departDate/(?P<dateTime>.+)/$',views.departureDate, name='departDate'),
    #url(r'^departTime/(?P<time>.+)/$',views.departureTime, name='departTime'),
)

