<?php
require_once 'db.php';
header('Content-Type: application/json');

$headers = apache_request_headers();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) {
    $token = $matches[1];
}

if (empty($token)) {
    $input = json_decode(file_get_contents('php://input'), true);
    $token = $input['token'] ?? '';
}

if (empty($token)) {
    echo json_encode(["status" => "error", "message" => "Unauthorized token missing"]);
    exit();
}

$stmt = $pdo->prepare("SELECT user_id, token_hash FROM user_sessions WHERE expires_at > NOW()");
$stmt->execute();
$sessions = $stmt->fetchAll();

$userId = null;
foreach ($sessions as $session) {
    if (password_verify($token, $session['token_hash'])) {
        $userId = $session['user_id'];
        break;
    }
}

if ($userId) {
    $stmt = $pdo->prepare("DELETE FROM user_sessions WHERE user_id = ?");
    $stmt->execute([$userId]);

    $stmt = $pdo->prepare("UPDATE users SET is_online = 0 WHERE id = ?");
    $stmt->execute([$userId]);

    echo json_encode(["status" => "success", "message" => "Logged out successfully"]);
} else {
    echo json_encode(["status" => "error", "message" => "Invalid or expired session"]);
}
?>