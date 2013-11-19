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
		
def nerTag(text):
	txt=nltk.word_tokenize(text)
        pos=nltk.pos_tag(txt)
        nerTags=st.tag(txt)
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
			out['ORIGIN']=out['LOCATION']
			del out['LOCATION']

	return str(pos)+str(out)

def parseDateTime(rawDatetime):
	return TZ.localize(datetime(*tuple(pdt.Calendar(Constants()).parse(rawDateTime)[0])[:7])) 
	
