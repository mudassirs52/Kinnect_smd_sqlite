<?php
require_once 'db.php';
header('Content-Type: application/json');
$headers = apache_request_headers();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) { $token = $matches[1]; }

$input = json_decode(file_get_contents('php://input'), true);
$fcmToken = $input['fcm_token'] ?? $_POST['fcm_token'] ?? '';

if (empty($token) || empty($fcmToken)) { echo json_encode(["status" => "error", "message" => "Invalid parameters"]); exit(); }

$stmt = $pdo->prepare("SELECT user_id FROM user_sessions WHERE expires_at > NOW()");
$stmt->execute();
$sessions = $stmt->fetchAll();
$userId = null;
foreach ($sessions as $session) { if (password_verify($token, $session['token_hash'])) { $userId = $session['user_id']; break; } }
if (!$userId) { echo json_encode(["status" => "error", "message" => "Unauthorized"]); exit(); }

$pdo->prepare("INSERT INTO device_tokens (user_id, fcm_token) VALUES (?, ?) ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), updated_at = NOW()")
    ->execute([$userId, $fcmToken]);
echo json_encode(["status" => "success", "message" => "Token registered"]);
?>