from django.http import HttpResponse
from django.utils import simplejson
from BeautifulSoup import BeautifulSoup
import re
import urllib2

def home(request, page = "news"):
    #fetch url
    page_contents = urllib2.urlopen("http://news.ycombinator.com/" + page)
    soup = BeautifulSoup(page_contents)
    resp = []
    for title in soup.findAll('td', {"class":"title"}):
        alink = title.findNext('a')
        if not alink.img:
            try:
                subtext = alink.parent.parent.nextSibling#.findNext('td',{'class':'subtext'})

                commentnr = unicode(subtext.span.nextSibling.nextSibling.nextSibling.nextSibling.contents[0]).split()[0]
                #show a zero if the parsing found "discuss" or anything not a number
                commentnr = commentnr if commentnr.isdigit() else "0"
                url = unicode(alink['href'])
                rematch = re.match("^item\?id=(\d+)$",url)
                if rematch:
                    url = ""
                resp.append({
                        'title': unicode(alink.contents[0]),
                        'link': url,
                        'points': unicode(subtext.span.contents[0]).split()[0],
                        'submitter': unicode(subtext.span.nextSibling.nextSibling.contents[0]).split()[0],
                        'comments': commentnr,
                        'id': unicode(subtext.span['id'])[6:],
                        })
            except AttributeError:
                pass
    #parse
    #return json
    jsonresp = simplejson.dumps(resp)
    return HttpResponse(jsonresp)

