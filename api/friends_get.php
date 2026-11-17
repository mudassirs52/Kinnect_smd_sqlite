<?php
require_once 'db.php';
header('Content-Type: application/json');
$headers = apache_request_headers();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) { $token = $matches[1]; }
$uid = $_GET['uid'] ?? 0;

if (empty($token)) { echo json_encode(["status" => "error", "message" => "Unauthorized"]); exit(); }

$stmt = $pdo->prepare("SELECT user_id FROM user_sessions WHERE expires_at > NOW()");
$stmt->execute();
$sessions = $stmt->fetchAll();
$userId = null;
foreach ($sessions as $session) { if (password_verify($token, $session['token_hash'])) { $userId = $session['user_id']; break; } }
if (!$userId) { echo json_encode(["status" => "error", "message" => "Unauthorized"]); exit(); }

$targetId = !empty($uid) ? $uid : $userId;
$stmt = $pdo->prepare("SELECT CASE WHEN user_id_1 = ? THEN user_id_2 ELSE user_id_1 END AS friend_id FROM friend_relationships WHERE user_id_1 = ? OR user_id_2 = ?");
$stmt->execute([$targetId, $targetId, $targetId]);
$friendIds = $stmt->fetchAll(PDO::FETCH_COLUMN);

$friends = [];
if (!empty($friendIds)) {
    $placeholders = implode(',', array_fill(0, count($friendIds), '?'));
    $stmtUsers = $pdo->prepare("SELECT id, first_name, last_name, username, profile_photo FROM users WHERE id IN ($placeholders)");
    $stmtUsers->execute($friendIds);
    $users = $stmtUsers->fetchAll();
    foreach ($users as $u) {
        $friends[] = [
            "id" => (string)$u['id'],
            "name" => $u['first_name'] . ' ' . $u['last_name'],
            "username" => $u['username'] ?? '',
            "profile_photo" => $u['profile_photo'] ?? ''
        ];
    }
}
echo json_encode(["status" => "success", "friends" => $friends]);
?>