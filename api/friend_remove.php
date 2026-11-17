<?php
require_once 'db.php';
header('Content-Type: application/json');
$headers = apache_request_headers();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) { $token = $matches[1]; }
$input = json_decode(file_get_contents('php://input'), true);
$otherId = $input['other_id'] ?? $_POST['other_id'] ?? 0;

if (empty($token) || empty($otherId)) { echo json_encode(["status" => "error", "message" => "Invalid parameters"]); exit(); }

$stmt = $pdo->prepare("SELECT user_id FROM user_sessions WHERE expires_at > NOW()");
$stmt->execute();
$sessions = $stmt->fetchAll();
$userId = null;
foreach ($sessions as $session) { if (password_verify($token, $session['token_hash'])) { $userId = $session['user_id']; break; } }
if (!$userId) { echo json_encode(["status" => "error", "message" => "Unauthorized"]); exit(); }

$pdo->prepare("DELETE FROM friend_relationships WHERE (user_id_1 = ? AND user_id_2 = ?) OR (user_id_1 = ? AND user_id_2 = ?)")->execute([min($userId, $otherId), max($userId, $otherId), min($userId, $otherId), max($userId, $otherId)]);
$pdo->prepare("DELETE FROM friend_requests WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)")->execute([$userId, $otherId, $otherId, $userId]);
echo json_encode(["status" => "success", "message" => "Friend removed"]);
?>