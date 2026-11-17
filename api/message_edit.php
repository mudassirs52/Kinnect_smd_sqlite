<?php
require_once 'db.php';
header('Content-Type: application/json');
$headers = apache_request_headers();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) { $token = $matches[1]; }

$input = json_decode(file_get_contents('php://input'), true);
$clientMsgId = $input['client_msg_id'] ?? $_POST['client_msg_id'] ?? '';
$newText = $input['new_text'] ?? $_POST['new_text'] ?? '';

if (empty($token) || empty($clientMsgId)) { echo json_encode(["status" => "error", "message" => "Invalid parameters"]); exit(); }

$stmt = $pdo->prepare("SELECT user_id FROM user_sessions WHERE expires_at > NOW()");
$stmt->execute();
$sessions = $stmt->fetchAll();
$userId = null;
foreach ($sessions as $session) { if (password_verify($token, $session['token_hash'])) { $userId = $session['user_id']; break; } }
if (!$userId) { echo json_encode(["status" => "error", "message" => "Unauthorized"]); exit(); }

$stmtMsg = $pdo->prepare("SELECT sender_id, TIMESTAMPDIFF(SECOND, created_at, NOW()) as age_seconds FROM messages WHERE client_msg_id = ?");
$stmtMsg->execute([$clientMsgId]);
$msg = $stmtMsg->fetch();

if (!$msg || $msg['sender_id'] != $userId) { echo json_encode(["status" => "error", "message" => "Message not found or unauthorized"]); exit(); }
if ($msg['age_seconds'] > 300) { echo json_encode(["status" => "error", "message" => "Edit window expired (5 minutes limit)"]); exit(); }

$pdo->prepare("UPDATE messages SET text = ?, edited = 1 WHERE client_msg_id = ?")->execute([$newText, $clientMsgId]);
echo json_encode(["status" => "success", "message" => "Message edited successfully"]);
?>