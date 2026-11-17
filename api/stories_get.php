<?php
require_once 'db.php';
header('Content-Type: application/json');

$headers = apache_request_headers();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) { $token = $matches[1]; }

if (empty($token)) { echo json_encode(["status" => "error", "message" => "Unauthorized"]); exit(); }

$pdo->exec("DELETE FROM stories WHERE expires_at <= NOW()");

$stmt = $pdo->prepare("SELECT s.*, u.first_name, u.last_name FROM stories s JOIN users u ON s.user_id = u.id WHERE s.expires_at > NOW() ORDER BY s.created_at DESC");
$stmt->execute();
$stories = $stmt->fetchAll();

$formatted = [];
foreach ($stories as $st) {
    $formatted[] = [
        "id" => (int)$st['id'],
        "user_id" => (string)$st['user_id'],
        "media_url" => $st['media_url'],
        "media_type" => $st['media_type'],
        "label" => $st['label'] ?? '',
        "audience" => $st['audience'],
        "expires_at" => strtotime($st['expires_at']) * 1000,
        "created_at" => strtotime($st['created_at']) * 1000,
        "author_name" => $st['first_name'] . ' ' . $st['last_name']
    ];
}
echo json_encode(["status" => "success", "stories" => $formatted]);
?>