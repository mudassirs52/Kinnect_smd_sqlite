<?php
require_once 'db.php';
header('Content-Type: application/json');
$headers = apache_request_headers();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) { $token = $matches[1]; }
$userIds = $_GET['uids'] ?? '';

if (empty($token)) { echo json_encode(["status" => "error", "message" => "Unauthorized"]); exit(); }
$ids = array_filter(explode(',', $userIds));
if (empty($ids)) { echo json_encode(["status" => "success", "presence" => []]); exit(); }

$placeholders = implode(',', array_fill(0, count($ids), '?'));
$stmt = $pdo->prepare("SELECT id, is_online, last_seen, TIMESTAMPDIFF(SECOND, last_seen, NOW()) as idle_sec FROM users WHERE id IN ($placeholders)");
$stmt->execute($ids);
$rows = $stmt->fetchAll();

$presence = [];
foreach ($rows as $r) {
    $isOnline = ($r['is_online'] == 1 && $r['idle_sec'] <= 60);
    $presence[$r['id']] = [ "is_online" => $isOnline, "last_seen" => $r['last_seen'] ];
}
echo json_encode(["status" => "success", "presence" => $presence]);
?>