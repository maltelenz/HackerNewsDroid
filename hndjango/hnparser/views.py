from django.http import HttpResponse
from django.utils import simplejson
from BeautifulSoup import BeautifulSoup
from BeautifulSoup import Tag
import re
import urllib2

###
# Fetch a page of news items, such as "news", "ask" or "best"
###
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

###
# Fetch a specific news item, with accompanying comments
###
def item(request, itemid):
    #fetch url
    page_contents = urllib2.urlopen("http://news.ycombinator.com/item?id=" + itemid)
    soup = BeautifulSoup(page_contents)
    resp = []
    tables = soup.findAll('table', {"border":"0"})
    form = tables[0].findNext('form',{"action":"/r", "method":"post"})
    title = soup.find('td', {"class" : "title"})
    
    postheader = title.parent.nextSibling.nextSibling.nextSibling.next.next
    #first row of text
    posttext = postheader.contents[0]#u''.join(postheader.findAll(text=True))
    if isinstance(posttext, Tag) and posttext.input:
        #not a local post, we do not have any text in the post itself
        posttext = ""
    #handle following paragraphs of text
    for hpart in postheader.parent.findAll('p'):
        posttext += '\n' + ''.join(hpart.findAll(text=True))

    try:
    #handle polls
        tr = postheader.parent.nextSibling.nextSibling.next.next.next.next
        while True:
            #vote alternative
            posttext += '\n' + u''.join(tr.find('td', {"class":"comment"}).findAll(text=True)) + ': '
            #nr of votes
            posttext += (u''.join(tr.nextSibling.find('span', {"class":"comhead"}).findAll(text=True)))
            if not tr.nextSibling.nextSibling.nextSibling:
                break
            tr = tr.nextSibling.nextSibling.nextSibling
    except AttributeError:
        #not a poll
        pass
    postname = soup.find('td', {"class" : "title"}).a.contents[0]
    postsubtext = soup.find('td', {"class" : "title"}).parent.nextSibling.find('td', {"class" : "subtext"})
    postpoints = postsubtext.span.contents[0].split()[0]
    postcomments = postsubtext.span.nextSibling.nextSibling.nextSibling.nextSibling.contents[0].split()[0]
    resp.append({
            "postname":postname,
            "posttext":posttext,
            "postpoints":postpoints,
            "postcomments":postcomments,
            })

    
    prevIndent = 0
    prevItemId = {
        -1 : "0",
        }
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
            itemId = unicode(newUnique())
            points = "0"
            poster = ""

        parent = prevItemId[indent-1]
        resp.append({
                "id" : itemId,
                "text" : commenttext,
                "parent" : parent,
                "points" : points,
                "poster" : poster,
                })
        
        prevIndent = indent
        prevItemId[indent] = itemId
    #return json
    jsonresp = simplejson.dumps(resp)
    return HttpResponse(jsonresp)

def newUnique(old = []):
    if old==[]:
        old = [12345678910]
    old[0] = old[0]+1
    return old[0]
