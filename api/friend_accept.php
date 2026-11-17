<?php
require_once 'db.php';
header('Content-Type: application/json');
$headers = apache_request_headers();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) { $token = $matches[1]; }
$input = json_decode(file_get_contents('php://input'), true);
$senderId = $input['sender_id'] ?? $_POST['sender_id'] ?? 0;

if (empty($token) || empty($senderId)) { echo json_encode(["status" => "error", "message" => "Invalid parameters"]); exit(); }

$stmt = $pdo->prepare("SELECT user_id FROM user_sessions WHERE expires_at > NOW()");
$stmt->execute();
$sessions = $stmt->fetchAll();
$userId = null;
foreach ($sessions as $session) { if (password_verify($token, $session['token_hash'])) { $userId = $session['user_id']; break; } }
if (!$userId) { echo json_encode(["status" => "error", "message" => "Unauthorized"]); exit(); }

$pdo->prepare("UPDATE friend_requests SET status = 'accepted' WHERE sender_id = ? AND receiver_id = ?")->execute([$senderId, $userId]);
$pdo->prepare("INSERT IGNORE INTO friend_relationships (user_id_1, user_id_2) VALUES (?, ?), (?, ?)")->execute([min($userId, $senderId), max($userId, $senderId), min($userId, $senderId), max($userId, $senderId)]);
echo json_encode(["status" => "success", "message" => "Friend request accepted"]);
?>