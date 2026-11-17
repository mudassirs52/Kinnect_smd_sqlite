<?php
require_once 'db.php';
header('Content-Type: application/json');
$headers = apache_request_headers();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) { $token = $matches[1]; }

$input = json_decode(file_get_contents('php://input'), true);
$callId = $input['call_id'] ?? $_POST['call_id'] ?? '';
$status = $input['status'] ?? $_POST['status'] ?? 'ended';
$durationSec = $input['duration_sec'] ?? $_POST['duration_sec'] ?? 0;

if (empty($token) || empty($callId)) { echo json_encode(["status" => "error", "message" => "Invalid parameters"]); exit(); }

$pdo->prepare("UPDATE call_logs SET status = ?, duration_sec = ? WHERE call_id = ?")->execute([$status, $durationSec, $callId]);
echo json_encode(["status" => "success", "message" => "Call updated"]);
?>