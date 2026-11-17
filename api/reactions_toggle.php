<?php
require_once 'db.php';
header('Content-Type: application/json');
$headers = apache_request_headers();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) { $token = $matches[1]; }

$input = json_decode(file_get_contents('php://input'), true);
$postId = $input['post_id'] ?? $_POST['post_id'] ?? 0;
$reactionType = $input['reaction_type'] ?? $_POST['reaction_type'] ?? '';

if (empty($token) || empty($postId) || empty($reactionType)) { echo json_encode(["status" => "error", "message" => "Invalid parameters"]); exit(); }

$stmt = $pdo->prepare("SELECT user_id FROM user_sessions WHERE expires_at > NOW()");
$stmt->execute();
$sessions = $stmt->fetchAll();
$userId = null;
foreach ($sessions as $session) { if (password_verify($token, $session['token_hash'])) { $userId = $session['user_id']; break; } }
if (!$userId) { echo json_encode(["status" => "error", "message" => "Unauthorized"]); exit(); }

$stmt = $pdo->prepare("SELECT id, reaction_type FROM reactions WHERE post_id = ? AND user_id = ?");
$stmt->execute([$postId, $userId]);
$existing = $stmt->fetch();

if ($existing) {
    if ($existing['reaction_type'] === $reactionType) {
        $pdo->prepare("DELETE FROM reactions WHERE id = ?")->execute([$existing['id']]);
        $action = "removed";
    } else {
        $pdo->prepare("UPDATE reactions SET reaction_type = ? WHERE id = ?")->execute([$reactionType, $existing['id']]);
        $action = "updated";
    }
} else {
    $pdo->prepare("INSERT INTO reactions (post_id, user_id, reaction_type) VALUES (?, ?, ?)")->execute([$postId, $userId, $reactionType]);
    $action = "added";
}
echo json_encode(["status" => "success", "action" => $action]);
?>