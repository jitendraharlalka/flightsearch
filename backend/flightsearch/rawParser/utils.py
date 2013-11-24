from nltk.tag.stanford import NERTagger
import parsedatetime as pdt
from parsedatetime import Constants
from pytz import timezone
from datetime import datetime,timedelta
from django.conf import settings
import nltk
import logging

st=NERTagger('/usr/share/stanford-ner/classifiers/english.muc.7class.distsim.crf.ser.gz','/usr/share/stanford-ner/stanford-ner.jar')

TZ = timezone(getattr(settings, "TIME_ZONE", "UTC"))
broadTimes=['morning','noon','evening','night','midnight']
meridian=[' a.m.',' p.m.',' am',' pm']
suffix=['st','nd','rd','th']

logger = logging.getLogger(__name__)
		
def nerTag(text):
	txt=nltk.word_tokenize(text)
        pos=nltk.pos_tag(txt)
        nerTags=st.tag(txt)
	logger.debug(pos)
	logger.debug(nerTags)
        #return (nerTags)
	out={}
	
	previous='O'
	temp=[]

	iter=0
	for tag in nerTags:
		flag=False
		if tag[1]!=previous:
			if len(temp)>0:
				try:
					if previous in ['LOCATION','ORGANIZATION'] and  pos[iter-len(temp)-1][1]=='TO':
						out['DESTINATION']=temp
						flag=True
				except:
					pass
				if not flag:
					out.setdefault(previous,[]).append(temp)
			temp=[]
		
		if tag[1]!='O':
			temp.append(tag[0])		

		previous=tag[1]
		iter+=1
	
	if out.has_key('DESTINATION') and out.has_key('LOCATION'):
		if len(out['LOCATION'])==1:
			out['ORIGIN']=out['LOCATION'][0]
			del out['LOCATION']
	
	if out.has_key('ORGANIZATION'):
		logger.debug(out['ORGANIZATION'])
		for org in out['ORGANIZATION']:
			out.setdefault('LOCATION',[]).append(org)
		logger.debug('LOCATION****')
		logger.debug(out['LOCATION'])
		del out['ORGANIZATION']

	return str(out)


def processDate(rawDate):
	formatted=parseDateTime(rawDate)
	date=formatted.strftime("%A, %d %B %Y")
	return date

def processTime(rawTime):
	formatted=parseDateTime(rawTime)
	
	flag1=0
	normalized=rawTime.lower()
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
		time1=format1.strftime("%H:%M")
		time2=format2.strftime("%H:%M")
		return time1+"-"+time2

	flag2=0
	for mrdn in meridian:
		try:
			idx=normalized.index(mrdn)
			flag2=1
			break
		except ValueError:
			pass
	if flag2==1:
		time=formatted.strftime("%H:%M")
		return time

def parseDateTime(rawDateTime):
	return TZ.localize(datetime(*tuple(pdt.Calendar(Constants()).parse(rawDateTime)[0])[:7])) 
	
