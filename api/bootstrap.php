<?php
declare(strict_types=1);
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');
header('X-Content-Type-Options: nosniff');
$config = file_exists(__DIR__.'/config.local.php') ? require __DIR__.'/config.local.php' : require __DIR__.'/config.example.php';
function now_ms(): int { return (int)floor(microtime(true)*1000); }
function answer(array $data, int $code = 200): never {
    http_response_code($code); echo json_encode($data, JSON_UNESCAPED_SLASHES|JSON_THROW_ON_ERROR); exit;
}
function fail(string $message, int $code = 400): never { throw new ApiError($message, $code); }
class ApiError extends RuntimeException {}
set_exception_handler(function(Throwable $e) {
    global $pdo;
    if (isset($pdo) && $pdo->inTransaction()) $pdo->rollBack();
    if ($e instanceof ApiError) answer(['status'=>'error','message'=>$e->getMessage()], $e->getCode());
    error_log((string)$e);
    answer(['status'=>'error','message'=>'Server error. Check the PHP log.'],500);
});
$pdo = new PDO(getenv('KINNECT_DSN') ?: $config['dsn'], getenv('KINNECT_DB_USER') ?: $config['user'], getenv('KINNECT_DB_PASSWORD') ?: $config['password'], [
    PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION, PDO::ATTR_DEFAULT_FETCH_MODE=>PDO::FETCH_ASSOC,
    PDO::ATTR_EMULATE_PREPARES=>false,
]);
function query(string $sql, array $args=[]): PDOStatement { global $pdo; $s=$pdo->prepare($sql); $s->execute($args); return $s; }
function user_view(array $u): array {
    unset($u['password_hash']); $u['name']=trim($u['first_name'].' '.$u['last_name']);
    $u['online']=(bool)$u['online'] && (int)$u['last_seen'] > now_ms()-65000;
    $u['profile_complete']=(bool)$u['profile_complete']; return $u;
}
function authenticated(): array {
    $header=$_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '';
    if (!preg_match('/^Bearer (\S+)$/i',$header,$m)) fail('Please log in.',401);
    $u=query('SELECT u.* FROM users u JOIN user_sessions s ON s.user_id=u.id WHERE s.token_hash=? AND s.expires_at>?',[hash('sha256',$m[1]),now_ms()])->fetch();
    if (!$u) fail('Your session has expired. Please log in.',401);
    return $u;
}
function session_for(int $id): array {
    $token=bin2hex(random_bytes(32)); $expires=now_ms()+30*86400000;
    query('INSERT INTO user_sessions VALUES (?,?,?)',[hash('sha256',$token),$id,$expires]);
    query('UPDATE users SET online=1,last_seen=? WHERE id=?',[now_ms(),$id]);
    return ['status'=>'success','token'=>$token,'expires_at'=>$expires,'user'=>user_view(query('SELECT * FROM users WHERE id=?',[$id])->fetch())];
}
function friend_state(int $me,int $other): string {
    $f=query('SELECT * FROM friendships WHERE (sender_id=? AND receiver_id=?) OR (sender_id=? AND receiver_id=?)',[$me,$other,$other,$me])->fetch();
    return !$f?'NONE':($f['status']==='accepted'?'FRIENDS':((int)$f['sender_id']===$me?'SENT':'RECEIVED'));
}
function can_see_post(int $me,int $id): void {
    $p=query('SELECT user_id FROM posts WHERE id=?',[$id])->fetch();
    if (!$p || ((int)$p['user_id']!==$me && friend_state($me,(int)$p['user_id'])!=='FRIENDS')) fail('Post not available.',404);
}
function notify_user(int $to,int $from,string $type,string $text): void {
    query('INSERT INTO notifications(user_id,sender_id,type,text,created_at) VALUES(?,?,?,?,?)',[$to,$from,$type,$text,now_ms()]);
}
function own_media(string $path): string {
    if ($path==='' || preg_match('~^uploads/[a-f0-9]{40}\.(jpg|png|webp|gif|mp4|mov|pdf|txt|zip)$~D',$path)) return $path;
    fail('Invalid media path.');
}
