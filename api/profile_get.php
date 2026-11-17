
<?php

require_once 'db.php';

header('Content-Type: application/json');

$headers = apache_request_headers();

$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';

$token = '';

if (preg_match('/Bearer\s+(\S+)/i', $authHeader, $matches)) {
    $token = $matches[1];
}

$uid = $_GET['uid'] ?? 0;

if (empty($token)) {
    echo json_encode([
        "status" => "error",
        "message" => "Unauthorized"
    ]);
    exit();
}

// Fetch both user_id and token_hash
$stmt = $pdo->prepare(
    "SELECT user_id, token_hash
     FROM user_sessions
     WHERE expires_at > NOW()"
);

$stmt->execute();

$sessions = $stmt->fetchAll(PDO::FETCH_ASSOC);

$userId = null;

foreach ($sessions as $session) {

    if (password_verify($token, $session['token_hash'])) {
        $userId = $session['user_id'];
        break;
    }
}

if (!$userId) {
    echo json_encode([
        "status" => "error",
        "message" => "Unauthorized"
    ]);
    exit();
}

$targetId = !empty($uid) ? $uid : $userId;

$stmt = $pdo->prepare(
    "SELECT id, first_name, last_name, email, username,
            bio, profile_photo, cover_photo, created_at
     FROM users
     WHERE id = ?"
);

$stmt->execute([$targetId]);

$user = $stmt->fetch(PDO::FETCH_ASSOC);

if ($user) {

    echo json_encode([
        "status" => "success",
        "user" => [
            "id" => (string)$user['id'],
            "name" => $user['first_name'] . ' ' . $user['last_name'],
            "email" => $user['email'],
            "username" => $user['username'] ?? '',
            "bio" => $user['bio'] ?? '',
            "profile_photo" => $user['profile_photo'] ?? '',
            "cover_photo" => $user['cover_photo'] ?? '',
            "created_at" => strtotime($user['created_at']) * 1000
        ]
    ]);

} else {

    echo json_encode([
        "status" => "error",
        "message" => "User not found"
    ]);

}

?>
