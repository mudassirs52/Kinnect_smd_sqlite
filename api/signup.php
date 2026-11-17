<?php
require_once 'db.php';
header('Content-Type: application/json');

$input = json_decode(file_get_contents('php://input'), true);
if (!$input) { $input = $_POST; }

$firstName = trim($input['first_name'] ?? '');
$lastName = trim($input['last_name'] ?? '');
$email = trim($input['email'] ?? '');
$password = $input['password'] ?? '';
$gender = trim($input['gender'] ?? '');
$birthday = trim($input['birthday'] ?? '');

if (empty($firstName) || empty($lastName) || empty($email) || empty($password)) {
    echo json_encode(["status" => "error", "message" => "All required fields must be filled"]);
    exit();
}

if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    echo json_encode(["status" => "error", "message" => "Invalid email format"]);
    exit();
}

$stmt = $pdo->prepare("SELECT id FROM users WHERE email = ?");
$stmt->execute([$email]);
if ($stmt->fetch()) {
    echo json_encode(["status" => "error", "message" => "Email already registered"]);
    exit();
}

$passwordHash = password_hash($password, PASSWORD_DEFAULT);

$stmt = $pdo->prepare("INSERT INTO users (first_name, last_name, email, password_hash, gender, birthday, is_online) VALUES (?, ?, ?, ?, ?, ?, 1)");
if ($stmt->execute([$firstName, $lastName, $email, $passwordHash, $gender, $birthday])) {
    $userId = $pdo->lastInsertId();
    $token = bin2hex(random_bytes(32));
    $tokenHash = password_hash($token, PASSWORD_DEFAULT);
    $expiresAt = date('Y-m-d H:i:s', strtotime('+7 days'));

    $stmt = $pdo->prepare("INSERT INTO user_sessions (user_id, token_hash, expires_at) VALUES (?, ?, ?)");
    $stmt->execute([$userId, $tokenHash, $expiresAt]);

    echo json_encode([
        "status" => "success",
        "message" => "Account created successfully",
        "token" => $token,
        "user" => [
            "id" => $userId,
            "first_name" => $firstName,
            "last_name" => $lastName,
            "email" => $email
        ]
    ]);
} else {
    echo json_encode(["status" => "error", "message" => "Registration failed"]);
}
?>