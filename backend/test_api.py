import json,urllib.request,urllib.parse,urllib.error,uuid,time,pathlib,subprocess
BASE=__import__('os').getenv('KINNECT_URL','http://127.0.0.1:8088/').rstrip('/')+'/';results=[]
def call(ep,params=None,token=None,code=200):
 data=None if params is None else urllib.parse.urlencode(params).encode(); req=urllib.request.Request(BASE+ep+'.php',data=data,headers={'Authorization':'Bearer '+token} if token else {})
 try:
  with urllib.request.urlopen(req,timeout=15) as r:status=r.status;body=json.load(r)
 except urllib.error.HTTPError as e:status=e.code;body=json.load(e)
 assert status==code,(ep,status,body)
 return body
def action(ep,p,t,code=200):return call(ep,dict(p,action_uuid=str(uuid.uuid4())),t,code)
def check(name,fn):fn();results.append(name);print('PASS',name)
suffix=uuid.uuid4().hex[:10]
a=call('signup',dict(first_name='Demo',last_name='One',email=f'a{suffix}@test.example',password='Secure123!'))
b=call('signup',dict(first_name='Demo',last_name='Two',email=f'b{suffix}@test.example',password='Secure123!'))
ta,tb=a['token'],b['token'];ia,ib=a['user']['id'],b['user']['id']
check('Invalid password rejected',lambda:call('login',dict(email=f'a{suffix}@test.example',password='wrong'),code=401))
check('Missing token rejected',lambda:call('profile_get',code=401))
check('Profile setup',lambda:action('profile_update',dict(name='Demo One',username='one_'+suffix,bio='SQLite test'),ta))
check('Username uniqueness',lambda:action('profile_update',dict(name='Demo Two',username='one_'+suffix,bio=''),tb,409))
u=str(uuid.uuid4());payload=dict(receiver_id=ib,action_uuid=u)
check('Friend request',lambda:call('friend_send',payload,ta))
check('Friend retry is idempotent',lambda:call('friend_send',payload,ta))
check('Friend acceptance',lambda:action('friend_accept',dict(sender_id=ia),tb))
assert call('friends_get',token=ta)['friends'][0]['id']==ib;results.append('Friends list')
post=action('posts_create',dict(text='Offline-first post'),ta)['post_id']
assert call('posts_get',token=tb)['posts'][0]['id']==post;results.append('Friends feed')
for reaction in ['like','love','haha','wow','sad','angry']:action('reactions_toggle',dict(post_id=post,reaction_type=reaction),tb)
assert call('posts_get',token=tb)['posts'][0]['reaction_count']==1;results.append('Six reactions replace one another')
comment=action('comments_create',dict(post_id=post,text='Comment'),tb)['comment_id']
reply=action('comments_create',dict(post_id=post,text='Reply',parent_id=comment),ta)['comment_id'];results.append('Comment replies')
check('Comment like',lambda:action('comment_like',dict(comment_id=comment,liked=1),ta))
mid=str(uuid.uuid4());mp=dict(receiver_id=ib,text='Queued message',client_msg_id=mid,action_uuid=mid)
check('Message send',lambda:call('message_send',mp,ta))
check('Lost-response retry',lambda:call('message_send',mp,ta))
# GET helper for parameterized URLs.
def get(ep,p,t):
 req=urllib.request.Request(BASE+ep+'.php?'+urllib.parse.urlencode(p),headers={'Authorization':'Bearer '+t})
 with urllib.request.urlopen(req,timeout=15) as r:return json.load(r)
assert len(get('messages_get',{'other_user_id':ia},tb)['messages'])==1;results.append('No duplicate message')
check('Other user cannot edit',lambda:action('message_edit',dict(client_msg_id=mid,new_text='bad'),tb,404))
check('Message edit',lambda:action('message_edit',dict(client_msg_id=mid,new_text='Edited text'),ta))
check('Screenshot system message',lambda:action('screenshot_report',dict(receiver_id=ib),ta))
# Move the fixture timestamp backwards to verify the server's edit window without waiting.
sql=f"UPDATE messages SET created_at=created_at-301000 WHERE client_msg_id='{mid}'"
subprocess.run([__import__('os').getenv('KINNECT_MYSQL_BIN',r'C:\xampp\mysql\bin\mysql.exe'),'-h','127.0.0.1','-P',__import__('os').getenv('KINNECT_MYSQL_PORT','3307'),'-u',__import__('os').getenv('KINNECT_MYSQL_USER','root'),'kinnect_i230507_i230017','-e',sql],check=True,capture_output=True)
check('Edit after five minutes rejected',lambda:action('message_edit',dict(client_msg_id=mid,new_text='late'),ta,422))
check('Delete after five minutes rejected',lambda:action('message_delete',dict(client_msg_id=mid),ta,422))
c=action('call_create',dict(callee_id=ib,type='video'),ta)['call'];assert get('calls_incoming',{},tb)['call']['id']==c['id'];results.append('Incoming call signaling')
check('Call accepted by recipient',lambda:action('call_update',dict(call_id=c['id'],status='accepted'),tb))
check('Call log ended',lambda:action('call_update',dict(call_id=c['id'],status='ended',duration_sec=12),ta))
check('Logout revokes token',lambda:call('logout',{},ta))
check('Revoked token rejected',lambda:call('profile_get',token=ta,code=401))
pathlib.Path(__file__).with_name('test-results.json').write_text(json.dumps({'passed':len(results),'checks':results},indent=2))
print('TOTAL',len(results))

import base64
suffix=uuid.uuid4().hex[:10]
a=call('signup',dict(first_name='Media',last_name='One',email=f'ma{suffix}@test.example',password='Secure123!'));b=call('signup',dict(first_name='Media',last_name='Two',email=f'mb{suffix}@test.example',password='Secure123!'))
ta,tb=a['token'],b['token'];ia,ib=a['user']['id'],b['user']['id']
action('friend_send',dict(receiver_id=ib),ta);action('friend_accept',dict(sender_id=ia),tb)
def multi(ep,params,files,t):
 boundary='KinnectTestBoundary';body=bytearray()
 for k,v in params.items():body.extend(f'--{boundary}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode())
 for field,name,mime,data in files:
  body.extend(f'--{boundary}\r\nContent-Disposition: form-data; name="{field}"; filename="{name}"\r\nContent-Type: {mime}\r\n\r\n'.encode());body.extend(data);body.extend(b'\r\n')
 body.extend(f'--{boundary}--\r\n'.encode())
 req=urllib.request.Request(BASE+ep+'.php',data=bytes(body),headers={'Authorization':'Bearer '+t,'Content-Type':'multipart/form-data; boundary='+boundary})
 with urllib.request.urlopen(req,timeout=20) as r:return json.load(r)
def get(ep,p,t):
 req=urllib.request.Request(BASE+ep+'.php?'+urllib.parse.urlencode(p),headers={'Authorization':'Bearer '+t})
 with urllib.request.urlopen(req,timeout=15) as r:return json.load(r)
png=base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aP8sAAAAASUVORK5CYII=')
u=str(uuid.uuid4());p=dict(text='Three images',action_uuid=u);files=[(f'files[{i}]',f'image{i}.png','image/png',png) for i in range(3)]
post=multi('posts_create',p,files,ta);assert multi('posts_create',p,files,ta)==post
feed=call('posts_get',token=tb)['posts'];assert len(feed)==1 and len(feed[0]['images'])==3;results.append('Multipart post + retry: one post, three images')
c=multi('comments_create',dict(post_id=post['post_id'],text='Image comment',action_uuid=str(uuid.uuid4())),[('file','c.png','image/png',png)],tb)
assert get('comments_get',dict(post_id=post['post_id']),ta)['comments'][0]['image_url'].startswith('uploads/');results.append('Image comment saved on filesystem')
file=multi('message_upload',dict(action_uuid=str(uuid.uuid4())),[('file','note.txt','text/plain',b'assignment attachment')],ta)
assert urllib.request.urlopen(BASE+file['media_url']).read()==b'assignment attachment';results.append('Chat attachment upload and download')
story=multi('stories_upload',dict(audience='close',label='Close friends test',action_uuid=str(uuid.uuid4())),[('file','s.png','image/png',png)],ta)
assert not call('stories_get',token=tb)['stories'];results.append('Close-friends story excludes other friends')
action('close_friend',dict(other_id=ib),ta)
assert len(call('stories_get',token=tb)['stories'])==1;results.append('Selected close friend can view story')
sql=f"UPDATE stories SET expires_at=1 WHERE id={int(story['story_id'])}"
subprocess.run([__import__('os').getenv('KINNECT_MYSQL_BIN',r'C:\xampp\mysql\bin\mysql.exe'),'-h','127.0.0.1','-P',__import__('os').getenv('KINNECT_MYSQL_PORT','3307'),'-u',__import__('os').getenv('KINNECT_MYSQL_USER','root'),'kinnect_i230507_i230017','-e',sql],check=True,capture_output=True)
assert not call('stories_get',token=tb)['stories'];results.append('Expired stories excluded by server')
c=action('call_create',dict(callee_id=ib,type='voice'),ta)['call'];auth=get('agora_token',dict(call_id=c['id']),ta)
assert auth['token'].startswith('007') and auth['uid']==ia;results.append('Server creates signed Agora token for authorized participant')
prior=json.loads(pathlib.Path(__file__).with_name('test-results.json').read_text());prior['checks']+=results;prior['passed']=len(prior['checks']);pathlib.Path(__file__).with_name('test-results.json').write_text(json.dumps(prior,indent=2))
for result in results:print('PASS',result)
