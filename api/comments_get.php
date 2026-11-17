<?php
require_once 'db.php';
header('Content-Type: application/json');
$postId = $_GET['post_id'] ?? 0;
if (empty($postId)) { echo json_encode(["status" => "error", "message" => "Missing post_id"]); exit(); }

$stmt = $pdo->prepare("SELECT c.*, u.first_name, u.last_name FROM comments c JOIN users u ON c.user_id = u.id WHERE c.post_id = ? ORDER BY c.created_at ASC");
$stmt->execute([$postId]);
$comments = $stmt->fetchAll();

$formatted = [];
foreach ($comments as $c) {
    $formatted[] = [
        "id" => (int)$c['id'],
        "post_id" => (int)$c['post_id'],
        "user_id" => (string)$c['user_id'],
        "author_name" => $c['first_name'] . ' ' . $c['last_name'],
        "parent_id" => $c['parent_id'] ? (int)$c['parent_id'] : null,
        "text" => $c['text'] ?? '',
        "image_url" => $c['image_url'],
        "created_at" => strtotime($c['created_at']) * 1000
    ];
}
echo json_encode(["status" => "success", "comments" => $formatted]);
?>