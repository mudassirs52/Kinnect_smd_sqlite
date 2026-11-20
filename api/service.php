<?php
declare(strict_types=1);
require_once __DIR__.'/bootstrap.php';
$endpoint=basename($_SERVER['SCRIPT_NAME'],'.php');
$input=$_POST;
if (str_contains($_SERVER['CONTENT_TYPE']??'','application/json')) $input=json_decode(file_get_contents('php://input'),true,512,JSON_THROW_ON_ERROR);
$p=array_merge($_GET,$input??[]);
$get=in_array($endpoint,['posts_get','stories_get','messages_get','friends_get','profile_get','search','presence_get','comments_get','chats_get','notifications_get','call_get','calls_incoming','agora_token','health']);
if ($_SERVER['REQUEST_METHOD']!==($get?'GET':'POST')) fail('Method not allowed.',405);
if ($endpoint==='health') answer(['status'=>'success','database'=>query('SELECT 1')->fetchColumn()==1]);
if ($endpoint==='signup') {
    $email=strtolower(trim($p['email']??'')); $pw=$p['password']??'';
    if (!filter_var($email,FILTER_VALIDATE_EMAIL) || strlen($pw)<8 || trim($p['first_name']??'')==='' || trim($p['last_name']??'')==='') fail('Name, valid email and a password of at least 8 characters are required.');
    if (query('SELECT id FROM users WHERE email=?',[$email])->fetch()) fail('Email is already registered.',409);
    query('INSERT INTO users(first_name,last_name,email,password_hash,gender,birthday,created_at) VALUES (?,?,?,?,?,?,?)',[
        trim($p['first_name']),trim($p['last_name']),$email,password_hash($pw,PASSWORD_DEFAULT),$p['gender']??'',($p['birthday']??'')?:null,now_ms()]);
    answer(session_for((int)$pdo->lastInsertId()));
}
if ($endpoint==='login') {
    $u=query('SELECT * FROM users WHERE email=?',[strtolower(trim($p['email']??''))])->fetch();
    if (!$u || !password_verify($p['password']??'',$u['password_hash'])) fail('Invalid email or password.',401);
    answer(session_for((int)$u['id']));
}
$user=authenticated(); $me=(int)$user['id'];
if ($endpoint==='logout') {
    $header=$_SERVER['HTTP_AUTHORIZATION']??'';
    query('DELETE FROM user_sessions WHERE token_hash=?',[hash('sha256',substr($header,7))]);
    query('UPDATE users SET online=0,last_seen=? WHERE id=?',[now_ms(),$me]);
    if(!empty($p['fcm_token']))query('DELETE FROM device_tokens WHERE token=? AND user_id=?',[$p['fcm_token'],$me]);
    answer(['status'=>'success']);
}
if ($endpoint==='heartbeat') {
    query('UPDATE users SET online=?,last_seen=? WHERE id=?',[($p['online']??'1')==='1'?1:0,now_ms(),$me]);answer(['status'=>'success']);
}
$uuid=$p['action_uuid']??'';
if (!$get) {
    if(!preg_match('/^[a-zA-Z0-9_-]{16,80}$/D',$uuid))fail('A unique action_uuid is required.');
    $pdo->beginTransaction();
    query('INSERT IGNORE INTO processed_actions(user_id,action_uuid,endpoint) VALUES (?,?,?)',[$me,$uuid,$endpoint]);
    $reserved=query('SELECT * FROM processed_actions WHERE user_id=? AND action_uuid=? FOR UPDATE',[$me,$uuid])->fetch();
    if($reserved['endpoint']!==$endpoint)fail('Action UUID reused for another operation.',409);
    if($reserved['response']!==null) { $pdo->commit();answer(json_decode($reserved['response'],true,512,JSON_THROW_ON_ERROR)); }
}
$r=['status'=>'success'];
switch ($endpoint) {
case 'logout':
    $token=preg_replace('/^Bearer\s+/i','',$_SERVER['HTTP_AUTHORIZATION']??'');
    query('DELETE FROM user_sessions WHERE user_id=? AND token_hash=?',[$me,hash('sha256',$token)]);
    query('DELETE FROM device_tokens WHERE user_id=?',[$me]);
    query('UPDATE users SET online=0,last_seen=? WHERE id=?',[now_ms(),$me]); break;
case 'heartbeat':
    query('UPDATE users SET online=?,last_seen=? WHERE id=?',[($p['online']??'1')==='0'?0:1,now_ms(),$me]); break;
case 'profile_get':
    $id=(int)($p['user_id']??$p['uid']??$me);
    $u=query('SELECT * FROM users WHERE id=?',[$id])->fetch(); if (!$u) fail('User not found.',404);
    $r['user']=user_view($u); if ($id!==$me) unset($r['user']['email'],$r['user']['birthday']);
    $r['user']['friend_state']=friend_state($me,$id);
    $r['user']['friend_count']=(int)query("SELECT COUNT(*) FROM friendships WHERE status='accepted' AND (sender_id=? OR receiver_id=?)",[$id,$id])->fetchColumn(); break;
case 'profile_update':
    $name=trim($p['name']??'');$username=strtolower(trim($p['username']??''));
    if ($name==='' || !preg_match('/^[a-z0-9_]{3,40}$/D',$username)) fail('Enter your name and a username (3-40 letters, digits or underscores).');
    if (query('SELECT id FROM users WHERE username=? AND id<>?',[$username,$me])->fetch()) fail('Username is already taken.',409);
    $parts=explode(' ',$name,2);
    query('UPDATE users SET first_name=?,last_name=?,username=?,bio=?,profile_complete=1 WHERE id=?',[$parts[0],$parts[1]??'',$username,$p['bio']??'',$me]);
    $r['user']=user_view(query('SELECT * FROM users WHERE id=?',[$me])->fetch()); break;
case 'profile_upload_image':
    $path=media_upload($p,true); $col=($p['type']??'profile')==='cover'?'cover_photo':'profile_photo';
    query("UPDATE users SET $col=? WHERE id=?",[$path,$me]);$r['image_url']=$path;break;
case 'search':
    $term='%'.($p['q']??'').'%'; $filter=$p['filter']??'everyone';$r['users']=[];
    foreach(query("SELECT * FROM users WHERE id<>? AND (username LIKE ? OR CONCAT(first_name,' ',last_name) LIKE ?) ORDER BY first_name,id LIMIT 100",[$me,$term,$term]) as $u) {
        $v=user_view($u);$v['friend_state']=friend_state($me,(int)$u['id']);
        unset($v['email'],$v['birthday']);
        if ($filter==='friends' && $v['friend_state']!=='FRIENDS') continue;
        if ($filter==='requests' && !in_array($v['friend_state'],['SENT','RECEIVED'])) continue;
        $r['users'][]=$v;
    } break;
case 'friend_send':
    $other=(int)($p['receiver_id']??0); require_peer($me,$other);
    if (friend_state($me,$other)!=='NONE') fail('A friendship or request already exists.',409);
    query('INSERT INTO friendships(sender_id,receiver_id,created_at) VALUES (?,?,?)',[$me,$other,now_ms()]);
    notify_user($other,$me,'friend_request','Sent you a friend request'); break;
case 'friend_accept':
    $other=(int)($p['sender_id']??0);
    $s=query("UPDATE friendships SET status='accepted' WHERE sender_id=? AND receiver_id=? AND status='pending'",[$other,$me]);
    if (!$s->rowCount()) fail('Request not found.',404); break;
case 'friend_delete': case 'friend_remove':
    $other=(int)($p['other_id']??0);
    query('DELETE FROM friendships WHERE (sender_id=? AND receiver_id=?) OR (sender_id=? AND receiver_id=?)',[$me,$other,$other,$me]);break;
case 'friends_get':
    $r['friends']=[];$r['requests']=[];$r['sent']=[];
    foreach(query('SELECT * FROM friendships WHERE sender_id=? OR receiver_id=?',[$me,$me]) as $f) {
        $id=(int)$f['sender_id']===$me?(int)$f['receiver_id']:(int)$f['sender_id'];
        $u=user_view(query('SELECT * FROM users WHERE id=?',[$id])->fetch());unset($u['email'],$u['birthday']);
        $u['friend_state']=$f['status']==='accepted'?'FRIENDS':((int)$f['sender_id']===$me?'SENT':'RECEIVED');
        $r[$u['friend_state']==='FRIENDS'?'friends':($u['friend_state']==='SENT'?'sent':'requests')][]=$u;
    }break;
case 'posts_create':
    $text=trim($p['text']??''); $paths=upload_images();
    if ($text==='' && !$paths) fail('Write something or add a photo.');
    query('INSERT INTO posts(user_id,client_uuid,text,created_at) VALUES (?,?,?,?)',[$me,$uuid,$text,now_ms()]);
    $id=(int)$pdo->lastInsertId();
    foreach($paths as $i=>$path) query('INSERT INTO post_images(post_id,image_url,image_index) VALUES (?,?,?)',[$id,$path,$i]);
    $r['post_id']=$id;break;
case 'posts_get':
    $r['posts']=[];
    foreach(query("SELECT p.*,CONCAT(u.first_name,' ',u.last_name) author_name FROM posts p JOIN users u ON u.id=p.user_id WHERE p.user_id=? OR p.user_id IN (SELECT IF(sender_id=?,receiver_id,sender_id) FROM friendships WHERE status='accepted' AND (sender_id=? OR receiver_id=?)) ORDER BY p.created_at DESC,p.id DESC LIMIT 200",[$me,$me,$me,$me]) as $post) {
        $post['images']=query('SELECT image_url FROM post_images WHERE post_id=? ORDER BY image_index',[$post['id']])->fetchAll(PDO::FETCH_COLUMN);
        $post['comment_count']=(int)query('SELECT COUNT(*) FROM comments WHERE post_id=?',[$post['id']])->fetchColumn();
        $post['my_reaction']=query('SELECT reaction_type FROM reactions WHERE post_id=? AND user_id=?',[$post['id'],$me])->fetchColumn()?:'';
        $post['reaction_count']=(int)query('SELECT COUNT(*) FROM reactions WHERE post_id=?',[$post['id']])->fetchColumn();$r['posts'][]=$post;
    }break;
case 'reactions_toggle':
    $id=(int)($p['post_id']??0);can_see_post($me,$id);$kind=$p['reaction_type']??$p['kind']??'';
    if ($kind!=='' && !in_array($kind,['like','love','haha','wow','sad','angry'])) fail('Invalid reaction.');
    // Set an explicit desired state rather than toggling on every retry.
    if ($kind==='') query('DELETE FROM reactions WHERE post_id=? AND user_id=?',[$id,$me]);
    else query('INSERT INTO reactions VALUES (?,?,?) ON DUPLICATE KEY UPDATE reaction_type=VALUES(reaction_type)',[$id,$me,$kind]);break;
case 'comments_create':
    $id=(int)($p['post_id']??0);can_see_post($me,$id);$parent=(int)($p['parent_id']??0);
    if ($parent && !query('SELECT id FROM comments WHERE id=? AND post_id=?',[$parent,$id])->fetch()) fail('Reply target not found.',404);
    $image=isset($_FILES['file'])?media_upload($p,true):'';
    if (trim($p['text']??'')==='' && $image==='') fail('Comment cannot be empty.');
    query('INSERT INTO comments(post_id,user_id,parent_id,text,image_url,created_at) VALUES (?,?,?,?,?,?)',[$id,$me,$parent?:null,$p['text']??'',$image,now_ms()]);$r['comment_id']=(int)$pdo->lastInsertId();break;
case 'comments_get':
    $id=(int)($p['post_id']??0);can_see_post($me,$id);
    $r['comments']=query("SELECT c.*,CONCAT(u.first_name,' ',u.last_name) author_name FROM comments c JOIN users u ON u.id=c.user_id WHERE post_id=? ORDER BY c.created_at,c.id",[$id])->fetchAll();
    foreach($r['comments'] as &$c) $c['liked']=(bool)query('SELECT 1 FROM comment_likes WHERE comment_id=? AND user_id=?',[$c['id'],$me])->fetchColumn();unset($c);break;
case 'comment_like':
    $id=(int)($p['comment_id']??0);$c=query('SELECT * FROM comments WHERE id=?',[$id])->fetch();if(!$c)fail('Comment not found.',404);can_see_post($me,(int)$c['post_id']);
    if (($p['liked']??'1')==='1') query('INSERT IGNORE INTO comment_likes VALUES (?,?)',[$id,$me]);
    else query('DELETE FROM comment_likes WHERE comment_id=? AND user_id=?',[$id,$me]);break;
case 'comment_delete':
    $id=(int)($p['comment_id']??0);$c=query('SELECT * FROM comments WHERE id=? AND user_id=?',[$id,$me])->fetch();if(!$c)fail('Comment not found.',404);
    query('DELETE FROM comments WHERE parent_id=? OR id=?',[$id,$id]);break;
case 'stories_upload':
    $path=media_upload($p);$audience=$p['audience']??'story';if(!in_array($audience,['story','all','close']))fail('Invalid audience.');
    $type=str_contains($path,'.mp4')||str_contains($path,'.mov')?'video':'image';
    query('INSERT INTO stories(user_id,media_url,media_type,label,audience,created_at,expires_at) VALUES (?,?,?,?,?,?,?)',[$me,$path,$type,$p['label']??'',$audience,now_ms(),now_ms()+86400000]);$r['story_id']=(int)$pdo->lastInsertId();break;
case 'stories_get':
    $r['stories']=[];
    foreach(query("SELECT s.*,CONCAT(u.first_name,' ',u.last_name) author_name FROM stories s JOIN users u ON u.id=s.user_id WHERE expires_at>? ORDER BY created_at",[now_ms()]) as $s) {
        if ((int)$s['user_id']===$me) {$r['stories'][]=$s;continue;}
        if(friend_state($me,(int)$s['user_id'])!=='FRIENDS')continue;
        if($s['audience']==='close' && !query('SELECT 1 FROM close_friends WHERE owner_id=? AND friend_id=?',[$s['user_id'],$me])->fetch())continue;
        $r['stories'][]=$s;
    }break;
case 'story_delete': query('DELETE FROM stories WHERE id=? AND user_id=?',[(int)($p['story_id']??0),$me]);break;
case 'close_friend':
    $other=(int)($p['other_id']??0);if(friend_state($me,$other)!=='FRIENDS')fail('Choose an existing friend.');
    // Direction is stored separately so each owner selects their own audience.
    query('INSERT INTO close_friends VALUES (?,?) ON DUPLICATE KEY UPDATE owner_id=VALUES(owner_id)',[$me,$other]);break;
case 'message_upload': $r['media_url']=media_upload($p);break;
case 'message_send': case 'screenshot_report':
    $other=(int)($p['receiver_id']??$p['other_user_id']??0);require_peer($me,$other);
    $id=$p['client_msg_id']??$uuid;$text=$p['text']??'';$media=own_media($p['media_url']??'');$type=$p['media_type']??'text';
    if ($endpoint==='screenshot_report') {$text=trim($user['first_name'].' '.$user['last_name']).' took a screenshot';$type='system';}
    if ($text==='' && $media==='') fail('Message cannot be empty.');
    $existing=query('SELECT * FROM messages WHERE client_msg_id=?',[$id])->fetch();
    if ($existing && (int)$existing['sender_id']!==$me) fail('Message ID conflict.',409);
    if (!$existing) {
        query('INSERT INTO messages(client_msg_id,sender_id,receiver_id,text,media_url,media_type,reply_id,created_at) VALUES (?,?,?,?,?,?,?,?)',[$id,$me,$other,$text,$media,$type,($p['reply_id']??'')?:null,now_ms()]);
        $r['message_id']=(int)$pdo->lastInsertId();notify_user($other,$me,$endpoint==='screenshot_report'?'screenshot':'message',$text?:'Sent an attachment');
    } else $r['message_id']=(int)$existing['id'];
    $r['client_msg_id']=$id;break;
case 'message_edit':case 'message_delete':
    $m=query('SELECT * FROM messages WHERE client_msg_id=? AND sender_id=? FOR UPDATE',[$p['client_msg_id']??'',$me])->fetch();
    if (!$m || $m['deleted']) fail('Message not found.',404);
    if (now_ms()-(int)$m['created_at']>300000) fail('Messages can only be changed within 5 minutes.',422);
    if ($endpoint==='message_delete') query("UPDATE messages SET deleted=1,text='',media_url='' WHERE id=?",[$m['id']]);
    else {if(trim($p['new_text']??'')==='')fail('Message cannot be empty.');query('UPDATE messages SET text=?,edited=1 WHERE id=?',[$p['new_text'],$m['id']]);}break;
case 'messages_get':
    $other=(int)($p['other_user_id']??0);$r['messages']=query('SELECT * FROM messages WHERE (sender_id=? AND receiver_id=?) OR (sender_id=? AND receiver_id=?) ORDER BY created_at,id',[$me,$other,$other,$me])->fetchAll();
    foreach($r['messages'] as &$m) {$m['edited']=(bool)$m['edited'];$m['deleted']=(bool)$m['deleted'];}unset($m);break;
case 'chats_get':
    $r['chats']=[];
    foreach(query('SELECT m.* FROM messages m JOIN (SELECT MAX(id) id FROM messages WHERE sender_id=? OR receiver_id=? GROUP BY IF(sender_id=?,receiver_id,sender_id)) latest ON m.id=latest.id ORDER BY m.created_at DESC',[$me,$me,$me]) as $m) {
        $id=(int)$m['sender_id']===$me?(int)$m['receiver_id']:(int)$m['sender_id'];$u=user_view(query('SELECT * FROM users WHERE id=?',[$id])->fetch());unset($u['email'],$u['birthday']);$r['chats'][]=['user'=>$u,'message'=>$m];
    }break;
case 'presence_get':
    $r['users']=[];foreach(explode(',',$p['ids']??'') as $id) {if(!ctype_digit($id))continue;$u=query('SELECT * FROM users WHERE id=?',[(int)$id])->fetch();if($u)$r['users'][]=['id'=>$id,'online'=>user_view($u)['online']];}break;
case 'notifications_get':
    $r['notifications']=query("SELECT n.*,CONCAT(u.first_name,' ',u.last_name) sender_name FROM notifications n JOIN users u ON n.sender_id=u.id WHERE user_id=? ORDER BY n.created_at DESC LIMIT 100",[$me])->fetchAll();break;
case 'fcm_register':
    $token=$p['fcm_token']??'';if(strlen($token)<20 || strlen($token)>255)fail('Invalid device token.');
    query('INSERT INTO device_tokens VALUES (?,?) ON DUPLICATE KEY UPDATE user_id=VALUES(user_id)',[$token,$me]);break;
case 'call_create':
    $other=(int)($p['callee_id']??0);require_peer($me,$other);$id=$uuid;$channel='kn_'.str_replace('-','',$uuid);$type=$p['type']??'voice';if(!in_array($type,['voice','video']))fail('Invalid call type.');
    query("INSERT INTO calls(id,caller_id,callee_id,type,status,channel,created_at) VALUES (?,?,?,?,'ringing',?,?)",[$id,$me,$other,$type,$channel,now_ms()]);$r['call']=call_view(query('SELECT * FROM calls WHERE id=?',[$id])->fetch());break;
case 'call_update':
    $c=query('SELECT * FROM calls WHERE id=? AND (caller_id=? OR callee_id=?) FOR UPDATE',[$p['call_id']??'',$me,$me])->fetch();if(!$c)fail('Call not found.',404);
    $status=$p['status']??'';if(!in_array($status,['accepted','declined','ended','missed']))fail('Invalid call status.');
    if(in_array($c['status'],['declined','ended','missed']))break;
    if(in_array($status,['accepted','declined']) && (int)$c['callee_id']!==$me)fail('Only the recipient can answer.',403);
    query('UPDATE calls SET status=?,accepted_at=?,ended_at=?,duration_sec=? WHERE id=?',[$status,$status==='accepted'?now_ms():$c['accepted_at'],in_array($status,['ended','declined','missed'])?now_ms():0,max(0,(int)($p['duration_sec']??0)),$c['id']]);break;
case 'call_get':
    $c=query('SELECT * FROM calls WHERE id=? AND (caller_id=? OR callee_id=?)',[$p['call_id']??'',$me,$me])->fetch();$r['call']=$c?call_view($c):null;break;
case 'calls_incoming':
    query("UPDATE calls SET status='missed',ended_at=? WHERE callee_id=? AND status='ringing' AND created_at<?",[now_ms(),$me,now_ms()-45000]);
    $c=query("SELECT * FROM calls WHERE callee_id=? AND status='ringing' ORDER BY created_at DESC LIMIT 1",[$me])->fetch();$r['call']=$c?call_view($c):null;break;
case 'agora_token':
    $c=query('SELECT * FROM calls WHERE id=? AND (caller_id=? OR callee_id=?)',[$p['call_id']??'',$me,$me])->fetch();
    if(!$c || !in_array($c['status'],['ringing','accepted']))fail('Call not available.',404);
    if(empty($config['agora_app_id']) || empty($config['agora_certificate']))fail('Configure Agora App ID and certificate on the server.',503);
    require_once __DIR__.'/agora/RtcTokenBuilder2.php';
    $r['token']=RtcTokenBuilder2::buildTokenWithUid($config['agora_app_id'],$config['agora_certificate'],$c['channel'],$me,RtcTokenBuilder2::ROLE_PUBLISHER,3600,3600);
    $r['uid']=$me;$r['app_id']=$config['agora_app_id'];break;
default:fail('Endpoint not found.',404);
}
if ($pdo->inTransaction()) {
    query('UPDATE processed_actions SET response=? WHERE user_id=? AND action_uuid=?',[json_encode($r,JSON_THROW_ON_ERROR),$me,$uuid]);$pdo->commit();
}
answer($r);

function require_peer(int $me,int $other): void { if($me===$other || !query('SELECT id FROM users WHERE id=?',[$other])->fetch())fail('Choose another user.'); }
function media_upload(array $p,bool $image=false): string { if(!isset($_FILES['file']))fail('File is required.');return media_upload_file($_FILES['file'],$image); }
function media_upload_file(array $file,bool $image): string {
    if($file['error']!==UPLOAD_ERR_OK || $file['size']>25*1024*1024)fail('Upload failed or exceeds 25 MB.');
    $mime=(new finfo(FILEINFO_MIME_TYPE))->file($file['tmp_name']);
    $allowed=['image/jpeg'=>'jpg','image/png'=>'png','image/webp'=>'webp','image/gif'=>'gif','video/mp4'=>'mp4','video/quicktime'=>'mov','application/pdf'=>'pdf','text/plain'=>'txt','application/zip'=>'zip'];
    if(!isset($allowed[$mime]) || ($image && !str_starts_with($mime,'image/')))fail('Unsupported media type.');
    $dir=__DIR__.'/uploads';if(!is_dir($dir))mkdir($dir,0700,true);$name=bin2hex(random_bytes(20)).'.'.$allowed[$mime];
    if(!move_uploaded_file($file['tmp_name'],$dir.'/'.$name))fail('Could not save upload.',500);return 'uploads/'.$name;
}
function upload_images(): array {
    $files=$_FILES['files']??null;if(!$files)return [];if(count($files['name'])>10)fail('Maximum 10 images.');$paths=[];
    foreach($files['name'] as $i=>$name) $paths[]=media_upload_file(['name'=>$name,'tmp_name'=>$files['tmp_name'][$i],'size'=>$files['size'][$i],'error'=>$files['error'][$i]],true);return $paths;
}
function call_view(array $c): array {
    $caller=user_view(query('SELECT * FROM users WHERE id=?',[$c['caller_id']])->fetch());$callee=user_view(query('SELECT * FROM users WHERE id=?',[$c['callee_id']])->fetch());
    return $c+['caller_name'=>$caller['name'],'callee_name'=>$callee['name']];
}
