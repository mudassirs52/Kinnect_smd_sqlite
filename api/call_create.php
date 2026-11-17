<?php
require_once 'db.php';
header('Content-Type: application/json');
$headers = apache_request_headers();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) { $token = $matches[1]; }

$input = json_decode(file_get_contents('php://input'), true);
$callId = $input['call_id'] ?? $_POST['call_id'] ?? '';
$calleeId = $input['callee_id'] ?? $_POST['callee_id'] ?? 0;
$callType = $input['call_type'] ?? $_POST['call_type'] ?? 'voice';
$channel = $input['channel'] ?? $_POST['channel'] ?? $callId;

if (empty($token) || empty($callId) || empty($calleeId)) { echo json_encode(["status" => "error", "message" => "Invalid parameters"]); exit(); }

$stmt = $pdo->prepare("SELECT user_id FROM user_sessions WHERE expires_at > NOW()");
$stmt->execute();
$sessions = $stmt->fetchAll();
$userId = null;
foreach ($sessions as $session) { if (password_verify($token, $session['token_hash'])) { $userId = $session['user_id']; break; } }
if (!$userId) { echo json_encode(["status" => "error", "message" => "Unauthorized"]); exit(); }

$stmt = $pdo->prepare("INSERT INTO call_logs (call_id, caller_id, callee_id, call_type, status, channel) VALUES (?, ?, ?, ?, 'ringing', ?)");
if ($stmt->execute([$callId, $userId, $calleeId, $callType, $channel])) {
    echo json_encode(["status" => "success", "message" => "Call created"]);
} else {
    echo json_encode(["status" => "error", "message" => "Failed to create call log"]);
}
?>