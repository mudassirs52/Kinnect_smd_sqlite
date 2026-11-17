
<?php

require_once 'db.php';

header('Content-Type: application/json');

// Get authorization header
$headers = apache_request_headers();

$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';

$token = '';

if (preg_match('/Bearer\s+(\S+)/i', $authHeader, $matches)) {
    $token = $matches[1];
}

// Get input data
$input = json_decode(file_get_contents('php://input'), true) ?? [];

$name = $input['name'] ?? $_POST['name'] ?? '';
$username = $input['username'] ?? $_POST['username'] ?? '';
$bio = $input['bio'] ?? $_POST['bio'] ?? '';

if (empty($token)) {
    echo json_encode([
        "status" => "error",
        "message" => "Unauthorized"
    ]);
    exit();
}

// Get active sessions
$stmt = $pdo->prepare(
    "SELECT user_id, token_hash
     FROM user_sessions
     WHERE expires_at > NOW()"
);

$stmt->execute();

$sessions = $stmt->fetchAll(PDO::FETCH_ASSOC);

$userId = null;

foreach ($sessions as $session) {

    if (password_verify($token, $session['token_hash'])) {
        $userId = $session['user_id'];
        break;
    }
}

// Verify token
if (!$userId) {
    echo json_encode([
        "status" => "error",
        "message" => "Unauthorized"
    ]);
    exit();
}

// Split full name
$parts = explode(' ', trim($name), 2);

$firstName = $parts[0] ?? '';
$lastName = $parts[1] ?? '';

// Update profile
$stmt = $pdo->prepare(
    "UPDATE users
     SET first_name = ?,
         last_name = ?,
         username = ?,
         bio = ?
     WHERE id = ?"
);

$stmt->execute([
    $firstName,
    $lastName,
    $username,
    $bio,
    $userId
]);

echo json_encode([
    "status" => "success",
    "message" => "Profile updated successfully"
]);

?>
