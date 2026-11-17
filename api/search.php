<?php
require_once 'db.php';
header('Content-Type: application/json');
$headers = apache_request_headers();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) { $token = $matches[1]; }
$q = trim($_GET['q'] ?? '');
$filter = $_GET['filter'] ?? 'everyone';

if (empty($token)) { echo json_encode(["status" => "error", "message" => "Unauthorized"]); exit(); }

$stmt = $pdo->prepare("SELECT user_id FROM user_sessions WHERE expires_at > NOW()");
$stmt->execute();
$sessions = $stmt->fetchAll();
$userId = null;
foreach ($sessions as $session) { if (password_verify($token, $session['token_hash'])) { $userId = $session['user_id']; break; } }
if (!$userId) { echo json_encode(["status" => "error", "message" => "Unauthorized"]); exit(); }

$searchWildcard = '%' . $q . '%';
$stmt = $pdo->prepare("SELECT id, first_name, last_name, username, profile_photo FROM users WHERE id != ? AND (CONCAT(first_name, ' ', last_name) LIKE ? OR username LIKE ?) LIMIT 50");
$stmt->execute([$userId, $searchWildcard, $searchWildcard]);
$users = $stmt->fetchAll();

$formatted = [];
foreach ($users as $u) {
    $stmtRel = $pdo->prepare("SELECT COUNT(*) FROM friend_relationships WHERE (user_id_1 = ? AND user_id_2 = ?) OR (user_id_1 = ? AND user_id_2 = ?)");
    $stmtRel->execute([min($userId, $u['id']), max($userId, $u['id']), min($userId, $u['id']), max($userId, $u['id'])]);
    $isFriend = $stmtRel->fetchColumn() > 0;

    $stmtReq = $pdo->prepare("SELECT sender_id, status FROM friend_requests WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)");
    $stmtReq->execute([$userId, $u['id'], $u['id'], $userId]);
    $req = $stmtReq->fetch();

    $friendState = "NONE";
    if ($isFriend) {
        $friendState = "FRIENDS";
    } elseif ($req && $req['status'] === 'pending') {
        $friendState = ($req['sender_id'] == $userId) ? "SENT" : "RECEIVED";
    }

    $formatted[] = [
        "id" => (string)$u['id'],
        "name" => $u['first_name'] . ' ' . $u['last_name'],
        "username" => $u['username'] ?? '',
        "profile_photo" => $u['profile_photo'] ?? '',
        "friend_state" => $friendState
    ];
}
echo json_encode(["status" => "success", "users" => $formatted]);
?>