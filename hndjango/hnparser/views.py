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

def item(request, itemid):
    #fetch url
    page_contents = urllib2.urlopen("http://news.ycombinator.com/item?id=" + itemid)
    soup = BeautifulSoup(page_contents)
    resp = []
    tables = soup.findAll('table', {"border":"0"})
    form = tables[0].findNext('form',{"action":"/r", "method":"post"})
    posttext = form.parent.parent.previousSibling.previousSibling.next.next.contents
    resp.append({"posttext":posttext})

    prevIndent = 0;
    prevItemId = 0;
    for comment in soup.findAll('span',{"class":"comment"}):
        indent = int(comment.parent.previousSibling.previousSibling.img['width'])/40;
            
        if (comment.contents[0] != "[deleted]"):
            #not a deleted item
            commenttext = u''.join(comment.contents[0].findAll(text=True))
            for cpart in comment.parent.findAll('p')[:-1]:
                commenttext += '\n' + ''.join(cpart.findAll(text=True))
            
            itemId = comment.parent.previousSibling.center.a['id'][3:]
            
            points = comment.previousSibling.previousSibling.previousSibling.span.span.contents[0].split()[0]
            poster = comment.previousSibling.previousSibling.previousSibling.span.a.contents[0]
        else:
            #deleted item
            commenttext = "[deleted]"
            #deleted items have no itemId. We have to invent one, hopefully unique
            itemId = unicode(int(prevItemId)*100)
            points = "0"
            poster = ""

        parent = "0"
        if (indent > prevIndent):
            parent = prevItemId
        resp.append({
                "id" : itemId,
                "text" : commenttext,
                "parent" : parent,
                "points" : points,
                "poster" : poster,
                })
        
        prevIndent = indent
        prevItemId = itemId
    #return json
    jsonresp = simplejson.dumps(resp)
    return HttpResponse(jsonresp)

