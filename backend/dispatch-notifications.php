<?php
// Run from CLI every few seconds. Private service account lives outside the web root.
if(PHP_SAPI!=='cli'){http_response_code(404);exit;}
require __DIR__.'/../api/bootstrap.php';
$file=getenv('GOOGLE_APPLICATION_CREDENTIALS')?:$config['fcm_service_account'];
if(!$file || !is_file($file)){fwrite(STDERR,"Set fcm_service_account in api/config.local.php.\n");exit(1);}
$sa=json_decode(file_get_contents($file),true,512,JSON_THROW_ON_ERROR);
function b64url(string $data): string{return rtrim(strtr(base64_encode($data),'+/','-_'),'=');}
function http_json(string $url,array $headers,string $body): array {
    $ch=curl_init($url);curl_setopt_array($ch,[CURLOPT_POST=>true,CURLOPT_HTTPHEADER=>$headers,CURLOPT_POSTFIELDS=>$body,CURLOPT_RETURNTRANSFER=>true,CURLOPT_CONNECTTIMEOUT=>5,CURLOPT_TIMEOUT=>15]);
    $raw=curl_exec($ch);$code=curl_getinfo($ch,CURLINFO_HTTP_CODE);$error=curl_error($ch);curl_close($ch);
    if($raw===false || $code<200 || $code>=300)throw new RuntimeException("FCM HTTP $code: $error");return json_decode($raw,true,512,JSON_THROW_ON_ERROR);
}
$now=time();$unsigned=b64url(json_encode(['alg'=>'RS256','typ'=>'JWT'])).'.'.b64url(json_encode([
    'iss'=>$sa['client_email'],'scope'=>'https://www.googleapis.com/auth/firebase.messaging',
    'aud'=>'https://oauth2.googleapis.com/token','iat'=>$now,'exp'=>$now+3600,
]));
if(!openssl_sign($unsigned,$sig,$sa['private_key'],OPENSSL_ALGO_SHA256))throw new RuntimeException('Cannot sign service account assertion');
$auth=http_json('https://oauth2.googleapis.com/token',['Content-Type: application/x-www-form-urlencoded'],http_build_query(['grant_type'=>'urn:ietf:params:oauth:grant-type:jwt-bearer','assertion'=>$unsigned.'.'.b64url($sig)]));
$messages=query("SELECT n.*,CONCAT(u.first_name,' ',u.last_name) sender_name FROM notifications n JOIN users u ON u.id=n.sender_id WHERE delivered=0 ORDER BY id LIMIT 100")->fetchAll();
foreach($messages as $n) {
    $tokens=query('SELECT token FROM device_tokens WHERE user_id=?',[$n['user_id']])->fetchAll(PDO::FETCH_COLUMN);
    if(!$tokens)continue;
    $ok=true;
    foreach($tokens as $token)try {
        http_json('https://fcm.googleapis.com/v1/projects/'.rawurlencode($sa['project_id']).'/messages:send',[
            'Authorization: Bearer '.$auth['access_token'],'Content-Type: application/json',
        ],json_encode(['message'=>['token'=>$token,'data'=>[
            'type'=>(string)$n['type'],'sender_uid'=>(string)$n['sender_id'],'sender_name'=>$n['sender_name'],
            'text'=>$n['text'],'notification_id'=>(string)$n['id'],
        ],'android'=>['priority'=>'HIGH']]],JSON_THROW_ON_ERROR));
    }catch(Throwable $e){$ok=false;fwrite(STDERR,$e->getMessage()."\n");}
    if($ok)query('UPDATE notifications SET delivered=1 WHERE id=?',[$n['id']]);
}
echo "Processed ".count($messages)." notifications.\n";
