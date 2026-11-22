BEGIN;

SET LOCAL search_path TO food_bank, public;

-- 1. USERS

INSERT INTO users
(user_id, name, email, phone, password_hash, role)
VALUES
(1, 'Test Coordinator', 'coordinator@test.example', '6045550101', 'test123', 'Coordinator'),
(2, 'Test Volunteer', 'volunteer@test.example', '6045550102', 'test123', 'Volunteer'),
(3, 'Test Donor', 'donor@test.example', '6045550103', 'test123', 'Donor'),
(4, 'Test Recipient', 'recipient@test.example', '6045550104', 'test123', 'Recipient'),
(5, 'Pending Recipient', 'pending@test.example', '6045550105', 'test123', 'Recipient'),
(6, 'Second Volunteer', 'volunteer2@test.example', '6045550106', 'test123', 'Volunteer'),
(7, 'Second Donor', 'donor2@test.example', '6045550107', 'test123', 'Donor'),
(8, 'Second Recipient', 'recipient2@test.example', '6045550108', 'test123', 'Recipient');


-- 2. RECIPIENTS

INSERT INTO recipients
(recipient_id, household_size, dietary_restrictions,
 emergency_contact, approval_status, approved_by, approved_at)
VALUES
(4, 4, 'Vegetarian', '6045550201', 'Approved', 1, CURRENT_TIMESTAMP),
(5, 2, 'None', '6045550202', 'Pending', NULL, NULL),
(8, 3, 'Nut allergy', '6045550203', 'Approved', 1, CURRENT_TIMESTAMP);


-- 3. VOLUNTEERS

INSERT INTO volunteers
(volunteer_id, availability, created_by)
VALUES
(2, 'Monday to Friday', 1),
(6, 'Saturday and Sunday', 1);


-- 4. CATEGORIES

INSERT INTO categories
(category_id, name)
VALUES
(1, 'Grains'),
(2, 'Vegetables'),
(3, 'Dairy'),
(4, 'Canned Food'),
(5, 'Fruits'),
(6, 'Pulses');


-- 5. DONATIONS

INSERT INTO donations
(donation_id, donor_id, recorded_by, item_name,
 category_id, quantity, unit, donation_date, status)
VALUES
(1, 3, 2, 'Rice', 1, 100, 'kg',
 CURRENT_TIMESTAMP - INTERVAL '10 days', 'COMPLETED'),

(2, 7, 2, 'Potatoes', 2, 50, 'kg',
 CURRENT_TIMESTAMP - INTERVAL '5 days', 'COMPLETED'),

(3, 3, 6, 'Milk', 3, 30, 'litres',
 CURRENT_TIMESTAMP - INTERVAL '3 days', 'COMPLETED'),

(4, 7, 6, 'Canned Beans', 4, 60, 'cans',
 CURRENT_TIMESTAMP - INTERVAL '7 days', 'COMPLETED'),

(5, 3, 3, 'Apples', 5, 25, 'kg',
 CURRENT_TIMESTAMP, 'CREATED'),

(6, 7, 6, 'Lentils', 6, 40, 'kg',
 CURRENT_TIMESTAMP - INTERVAL '2 days', 'COMPLETED');


-- 6. INVENTORY

INSERT INTO inventory
(inventory_id, donation_id, item_name, category_id,
 quantity, quantity_received, unit, received_date,
 expiry_date, waste_date, status, updated_by)
VALUES
(1, 1, 'Rice', 1, 90, 100, 'kg',
 CURRENT_DATE - 10, CURRENT_DATE + 180,
 NULL, 'Available', 2),

(2, 2, 'Potatoes', 2, 40, 50, 'kg',
 CURRENT_DATE - 5, CURRENT_DATE + 10,
 NULL, 'Available', 2),

(3, 3, 'Milk', 3, 30, 30, 'litres',
 CURRENT_DATE - 3, CURRENT_DATE + 1,
 NULL, 'Available', 6),

(4, 4, 'Canned Beans', 4, 60, 60, 'cans',
 CURRENT_DATE - 7, CURRENT_DATE + 365,
 NULL, 'Available', 6),

(5, 6, 'Lentils', 6, 40, 40, 'kg',
 CURRENT_DATE - 2, CURRENT_DATE + 90,
 NULL, 'Available', 6),

(6, NULL, 'Expired Yogurt', 3, 0, 10, 'units',
 CURRENT_DATE - 15, CURRENT_DATE - 2,
 CURRENT_DATE - 1, 'Expired', 2),

(7, NULL, 'Damaged Tomatoes', 2, 0, 5, 'kg',
 CURRENT_DATE - 5, CURRENT_DATE + 2,
 CURRENT_DATE - 1, 'Discarded', 2);


-- 7. HAMPER REQUESTS

INSERT INTO hamper_requests
(request_id, recipient_id, request_date, pickup_date, status)
VALUES
(1, 4, CURRENT_DATE - 6, CURRENT_DATE - 4, 'FULFILLED'),
(2, 8, CURRENT_DATE - 3, CURRENT_DATE - 1, 'SCHEDULED'),
(3, 4, CURRENT_DATE, CURRENT_DATE + 2, 'SCHEDULED'),
(4, 8, CURRENT_DATE, CURRENT_DATE + 3, 'SCHEDULED'),
(5, 4, CURRENT_DATE, NULL, 'PENDING'),
(6, 8, CURRENT_DATE - 2, NULL, 'CANCELLED');


-- 8. DISTRIBUTIONS

INSERT INTO distributions
(distribution_id, request_id, volunteer_id,
 distribution_date, pickup_time, pickup_status)
VALUES
(1, 1, 2, CURRENT_DATE - 4, '10:00', 'FULFILLED'),
(2, 2, 2, CURRENT_DATE - 1, '11:00', 'MISSED'),
(3, 3, 2, CURRENT_DATE + 2, '10:00', 'SCHEDULED'),
(4, 4, 6, CURRENT_DATE + 3, '14:00', 'SCHEDULED');


-- 9. DISTRIBUTION ITEMS

INSERT INTO distribution_items
(distribution_item_id, distribution_id,
 inventory_id, quantity, unit)
VALUES
(1, 1, 1, 10, 'kg'),
(2, 1, 2, 10, 'kg'),
(3, 2, 4, 5, 'cans'),
(4, 3, 1, 5, 'kg'),
(5, 3, 5, 2, 'kg'),
(6, 4, 4, 5, 'cans');


-- 10. VOLUNTEER SHIFTS

INSERT INTO volunteer_shifts
(shift_id, volunteer_id, coordinator_id,
 shift_date, start_time, end_time, task)
VALUES
(1, 2, 1, CURRENT_DATE, '09:00', '12:00',
 'Receive donations'),

(2, 2, 1, CURRENT_DATE, '13:00', '16:00',
 'Manage inventory'),

(3, 6, 1, CURRENT_DATE + 1, '10:00', '14:00',
 'Prepare food hampers');


-- 11. NOTIFICATIONS

INSERT INTO notifications
(notification_id, recipient_user_id, recipient_email,
 notification_type, reason, category_id, message,
 created_at, is_sent, sent_at)
VALUES
(1, 1, 'coordinator@test.example',
 'INVENTORY_AT_RISK', 'ExpiryRisk', 3,
 'Milk is approaching its expiry date.',
 CURRENT_TIMESTAMP, TRUE, CURRENT_TIMESTAMP),

(2, 1, 'coordinator@test.example',
 'MISSED_PICKUP', NULL, NULL,
 'Recipient missed the scheduled pickup.',
 CURRENT_TIMESTAMP, TRUE, CURRENT_TIMESTAMP);


-- 12. INVENTORY HISTORY

INSERT INTO inventory_history
(history_id, inventory_id, category_id, unit,
 quantity_change, change_type, occurred_at)
VALUES
(1, 1, 1, 'kg', 100, 'DONATION_RECEIVED',
 CURRENT_TIMESTAMP - INTERVAL '10 days'),

(2, 1, 1, 'kg', -10, 'DISTRIBUTED',
 CURRENT_TIMESTAMP - INTERVAL '4 days'),

(3, 2, 2, 'kg', 50, 'DONATION_RECEIVED',
 CURRENT_TIMESTAMP - INTERVAL '5 days'),

(4, 2, 2, 'kg', -10, 'DISTRIBUTED',
 CURRENT_TIMESTAMP - INTERVAL '4 days'),

(5, 3, 3, 'litres', 30, 'DONATION_RECEIVED',
 CURRENT_TIMESTAMP - INTERVAL '3 days'),

(6, 4, 4, 'cans', 60, 'DONATION_RECEIVED',
 CURRENT_TIMESTAMP - INTERVAL '7 days'),

(7, 5, 6, 'kg', 40, 'DONATION_RECEIVED',
 CURRENT_TIMESTAMP - INTERVAL '2 days'),

(8, 6, 3, 'units', 10, 'MANUAL_ADJUSTMENT',
 CURRENT_TIMESTAMP - INTERVAL '15 days'),

(9, 6, 3, 'units', -10, 'WASTED',
 CURRENT_TIMESTAMP - INTERVAL '1 day'),

(10, 7, 2, 'kg', 5, 'MANUAL_ADJUSTMENT',
 CURRENT_TIMESTAMP - INTERVAL '5 days'),

(11, 7, 2, 'kg', -5, 'WASTED',
 CURRENT_TIMESTAMP - INTERVAL '1 day');


SELECT setval(pg_get_serial_sequence('food_bank.users', 'user_id'),
    COALESCE((SELECT MAX(user_id) FROM food_bank.users), 0) + 1, false);

SELECT setval(pg_get_serial_sequence('food_bank.categories', 'category_id'),
    COALESCE((SELECT MAX(category_id) FROM food_bank.categories), 0) + 1, false);

SELECT setval(pg_get_serial_sequence('food_bank.donations', 'donation_id'),
    COALESCE((SELECT MAX(donation_id) FROM food_bank.donations), 0) + 1, false);

SELECT setval(pg_get_serial_sequence('food_bank.inventory', 'inventory_id'),
    COALESCE((SELECT MAX(inventory_id) FROM food_bank.inventory), 0) + 1, false);

SELECT setval(pg_get_serial_sequence('food_bank.hamper_requests', 'request_id'),
    COALESCE((SELECT MAX(request_id) FROM food_bank.hamper_requests), 0) + 1, false);

SELECT setval(pg_get_serial_sequence('food_bank.distributions', 'distribution_id'),
    COALESCE((SELECT MAX(distribution_id) FROM food_bank.distributions), 0) + 1, false);

SELECT setval(pg_get_serial_sequence('food_bank.distribution_items', 'distribution_item_id'),
    COALESCE((SELECT MAX(distribution_item_id) FROM food_bank.distribution_items), 0) + 1, false);

SELECT setval(pg_get_serial_sequence('food_bank.volunteer_shifts', 'shift_id'),
    COALESCE((SELECT MAX(shift_id) FROM food_bank.volunteer_shifts), 0) + 1, false);

SELECT setval(pg_get_serial_sequence('food_bank.notifications', 'notification_id'),
    COALESCE((SELECT MAX(notification_id) FROM food_bank.notifications), 0) + 1, false);

SELECT setval(pg_get_serial_sequence('food_bank.inventory_history', 'history_id'),
    COALESCE((SELECT MAX(history_id) FROM food_bank.inventory_history), 0) + 1, false);

COMMIT;
