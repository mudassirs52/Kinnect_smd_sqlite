<?php
require_once 'db.php';
header('Content-Type: application/json');
$headers = apache_request_headers();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) { $token = $matches[1]; }

$input = json_decode(file_get_contents('php://input'), true);
$otherUserId = $input['other_user_id'] ?? $_POST['other_user_id'] ?? 0;
$chatId = $input['chat_id'] ?? $_POST['chat_id'] ?? '';

if (empty($token) || empty($otherUserId)) { echo json_encode(["status" => "error", "message" => "Invalid parameters"]); exit(); }

$stmt = $pdo->prepare("SELECT user_id, first_name, last_name FROM user_sessions s JOIN users u ON s.user_id = u.id WHERE s.expires_at > NOW()");
$stmt->execute();
$sessions = $stmt->fetchAll();
$userId = null;
foreach ($sessions as $session) { if (password_verify($token, $session['token_hash'])) { $userId = $session['user_id']; break; } }
if (!$userId) { echo json_encode(["status" => "error", "message" => "Unauthorized"]); exit(); }

$pdo->prepare("INSERT INTO screenshot_events (chat_id, user_id, other_user_id) VALUES (?, ?, ?)")->execute([$chatId, $userId, $otherUserId]);
echo json_encode(["status" => "success", "message" => "Screenshot reported"]);
?>