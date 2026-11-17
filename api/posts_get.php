<?php
require_once 'db.php';
header('Content-Type: application/json');

$headers = apache_request_headers();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) { $token = $matches[1]; }

if (empty($token)) { echo json_encode(["status" => "error", "message" => "Unauthorized"]); exit(); }

$stmt = $pdo->prepare("SELECT p.*, u.first_name, u.last_name FROM posts p JOIN users u ON p.user_id = u.id ORDER BY p.created_at DESC");
$stmt->execute();
$posts = $stmt->fetchAll();

$formatted = [];
foreach ($posts as $p) {
    $stmtImg = $pdo->prepare("SELECT image_url FROM post_images WHERE post_id = ? ORDER BY image_index ASC");
    $stmtImg->execute([$p['id']]);
    $images = $stmtImg->fetchAll(PDO::FETCH_COLUMN);

    $stmtReact = $pdo->prepare("SELECT reaction_type, COUNT(*) as count FROM reactions WHERE post_id = ? GROUP BY reaction_type");
    $stmtReact->execute([$p['id']]);
    $reactions = $stmtReact->fetchAll(PDO::FETCH_KEY_PAIR);

    $stmtCom = $pdo->prepare("SELECT COUNT(*) FROM comments WHERE post_id = ?");
    $stmtCom->execute([$p['id']]);
    $commentCount = $stmtCom->fetchColumn();

    $formatted[] = [
        "id" => (int)$p['id'],
        "user_id" => (string)$p['user_id'],
        "author_name" => $p['first_name'] . ' ' . $p['last_name'],
        "text" => $p['text'] ?? '',
        "created_at" => strtotime($p['created_at']) * 1000,
        "images" => $images,
        "reactions" => $reactions,
        "comment_count" => (int)$commentCount
    ];
}
echo json_encode(["status" => "success", "posts" => $formatted]);
?>