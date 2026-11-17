<?php
require_once 'db.php';
header('Content-Type: application/json');

$input = json_decode(file_get_contents('php://input'), true);
if (!$input) { $input = $_POST; }

$email = trim($input['email'] ?? '');
$password = $input['password'] ?? '';

if (empty($email) || empty($password)) {
    echo json_encode(["status" => "error", "message" => "Email and password are required"]);
    exit();
}

$stmt = $pdo->prepare("SELECT * FROM users WHERE email = ?");
$stmt->execute([$email]);
$user = $stmt->fetch();

if ($user && password_verify($password, $user['password_hash'])) {
    $stmt = $pdo->prepare("UPDATE users SET is_online = 1 WHERE id = ?");
    $stmt->execute([$user['id']]);

    $token = bin2hex(random_bytes(32));
    $tokenHash = password_hash($token, PASSWORD_DEFAULT);
    $expiresAt = date('Y-m-d H:i:s', strtotime('+7 days'));

    $stmt = $pdo->prepare("INSERT INTO user_sessions (user_id, token_hash, expires_at) VALUES (?, ?, ?)");
    $stmt->execute([$user['id'], $tokenHash, $expiresAt]);

    echo json_encode([
        "status" => "success",
        "message" => "Login successful",
        "token" => $token,
        "user" => [
            "id" => $user['id'],
            "first_name" => $user['first_name'],
            "last_name" => $user['last_name'],
            "email" => $user['email']
        ]
    ]);
} else {
    echo json_encode(["status" => "error", "message" => "Invalid email or password"]);
}
?>