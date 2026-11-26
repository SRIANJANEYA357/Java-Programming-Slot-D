import urllib.request
import urllib.error
import json
import sys
import datetime
import ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

results = []

def record(test_name, passed, details):
    status_str = "PASS" if passed else "FAIL"
    results.append({"name": test_name, "status": status_str, "details": details})
    print(f"[{status_str}] {test_name}: {details}")

def make_request(url, method="GET", headers=None, data=None):
    if headers is None:
        headers = {}
    if data is not None and isinstance(data, dict):
        body = json.dumps(data).encode("utf-8")
        headers["Content-Type"] = "application/json"
    elif data is not None and isinstance(data, str):
        body = data.encode("utf-8")
    else:
        body = None

    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=20) as resp:
            resp_body = resp.read().decode("utf-8")
            try:
                parsed = json.loads(resp_body)
            except Exception:
                parsed = resp_body
            return resp.status, parsed, None
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8")
        try:
            parsed = json.loads(err_body)
        except Exception:
            parsed = err_body
        return e.code, parsed, e
    except Exception as e:
        return 0, None, e

def run_tests(target_base):
    print(f"\n=======================================================")
    print(f"RUNNING FOODSHARE PRODUCTION E2E SUITE ON: {target_base}")
    print(f"=======================================================\n")
    
    # 1. Health check
    code, body, err = make_request(f"{target_base}/actuator/health")
    if code == 200 and isinstance(body, dict) and body.get("status") == "UP":
        record("Actuator Health Check", True, f"Status: {body.get('status')}, groups: {body.get('groups')}")
    else:
        record("Actuator Health Check", False, f"Code: {code}, Resp: {body}, Err: {err}")

    # 2. Prevent Admin registration privilege escalation
    reg_admin_payload = {
        "fullName": "Malicious Attacker",
        "email": f"attacker_{int(datetime.datetime.now().timestamp())}@foodshare.com",
        "password": "Password123!",
        "phone": "9998887776",
        "role": "ROLE_ADMIN",
        "providerType": "NONE"
    }
    code, body, err = make_request(f"{target_base}/api/auth/register", method="POST", data=reg_admin_payload)
    if code in [400, 403]:
        record("Admin Registration Privilege Escalation Blocked", True, f"Code {code}: Successfully blocked unauthenticated user from registering as ROLE_ADMIN")
    else:
        record("Admin Registration Privilege Escalation Blocked", False, f"Code: {code}, unexpected acceptance of ROLE_ADMIN registration!")

    # 3. Register Provider
    ts = int(datetime.datetime.now().timestamp())
    provider_email = f"provider_{ts}@foodshare.org"
    provider_pass = "ProviderPass123!"
    reg_prov_payload = {
        "fullName": "Green Harvest Organics",
        "email": provider_email,
        "password": provider_pass,
        "phone": "9876543210",
        "role": "ROLE_PROVIDER",
        "providerType": "RESTAURANT",
        "organizationName": "Green Harvest Ltd",
        "address": "123 Farm Way",
        "city": "Metropolis"
    }
    code, body, err = make_request(f"{target_base}/api/auth/register", method="POST", data=reg_prov_payload)
    prov_token = None
    if code in [200, 201] and isinstance(body, dict) and "token" in body:
        prov_token = body["token"]
        record("Provider Public Registration", True, f"Created {provider_email} with JWT token received")
    else:
        record("Provider Public Registration", False, f"Code {code}, resp: {body}")

    # 4. Register Recipient
    recipient_email = f"recipient_{ts}@foodshare.org"
    recipient_pass = "RecipientPass123!"
    reg_recip_payload = {
        "fullName": "Community Food Bank",
        "email": recipient_email,
        "password": recipient_pass,
        "phone": "9123456780",
        "role": "ROLE_RECIPIENT",
        "providerType": "NONE",
        "organizationName": "Community Outreach Shelter",
        "address": "456 Hope Street",
        "city": "Metropolis"
    }
    code, body, err = make_request(f"{target_base}/api/auth/register", method="POST", data=reg_recip_payload)
    recip_token = None
    if code in [200, 201] and isinstance(body, dict) and "token" in body:
        recip_token = body["token"]
        record("Recipient Public Registration", True, f"Created {recipient_email} with JWT token received")
    else:
        record("Recipient Public Registration", False, f"Code {code}, resp: {body}")

    # 5. Admin Login
    admin_login_payload = {
        "email": "admin@foodshare.com",
        "password": "Admin@FoodShare2026!"
    }
    code, body, err = make_request(f"{target_base}/api/auth/login", method="POST", data=admin_login_payload)
    admin_token = None
    if code == 200 and isinstance(body, dict) and "token" in body:
        admin_token = body["token"]
        record("Admin Account Authentication", True, f"Successfully authenticated admin@foodshare.com, Role: {body.get('role')}")
    else:
        record("Admin Account Authentication", False, f"Code: {code}, resp: {body}, err: {err}")

    if not prov_token:
        c, b, _ = make_request(f"{target_base}/api/auth/login", method="POST", data={"email": provider_email, "password": provider_pass})
        if c == 200 and "token" in b: prov_token = b["token"]
    if not recip_token:
        c, b, _ = make_request(f"{target_base}/api/auth/login", method="POST", data={"email": recipient_email, "password": recipient_pass})
        if c == 200 and "token" in b: recip_token = b["token"]

    prov_headers = {"Authorization": f"Bearer {prov_token}"} if prov_token else {}
    recip_headers = {"Authorization": f"Bearer {recip_token}"} if recip_token else {}
    admin_headers = {"Authorization": f"Bearer {admin_token}"} if admin_token else {}

    # 6. Create Free Food Listing (10 units)
    future_time = (datetime.datetime.now() + datetime.timedelta(days=3)).strftime("%Y-%m-%dT%H:%M:%S")
    free_listing_payload = {
        "title": "Fresh Garden Salad & Soup",
        "description": "Nutritious garden salad boxes with whole grain rolls",
        "category": "COOKED_MEALS",
        "quantity": 10,
        "unit": "MEALS",
        "foodType": "FREE_DONATION",
        "originalPrice": 0.0,
        "discountedPrice": 0.0,
        "locationAddress": "123 Farm Way",
        "locationCity": "Metropolis",
        "expiryTime": future_time
    }
    code, body, err = make_request(f"{target_base}/api/listings", method="POST", headers=prov_headers, data=free_listing_payload)
    free_listing_id = None
    if code in [200, 201] and isinstance(body, dict) and "id" in body:
        free_listing_id = body["id"]
        record("Create Free Food Listing (Quantity: 10)", True, f"Listing ID: {free_listing_id}, Category: {body.get('category')}, FoodType: {body.get('foodType')}")
    else:
        record("Create Free Food Listing (Quantity: 10)", False, f"Code {code}, resp: {body}")

    # 7. Create Discounted Food Listing (5 units)
    discount_listing_payload = {
        "title": "Artisan Sourdough Loaves",
        "description": "Day-fresh sourdough bread loaves at 60% discount",
        "category": "BAKERY",
        "quantity": 5,
        "unit": "MEALS",
        "foodType": "DISCOUNTED_SALE",
        "originalPrice": 8.0,
        "discountedPrice": 3.0,
        "locationAddress": "123 Farm Way",
        "locationCity": "Metropolis",
        "expiryTime": future_time
    }
    code, body, err = make_request(f"{target_base}/api/listings", method="POST", headers=prov_headers, data=discount_listing_payload)
    discount_listing_id = None
    if code in [200, 201] and isinstance(body, dict) and "id" in body:
        discount_listing_id = body["id"]
        record("Create Discounted Food Listing", True, f"Listing ID: {discount_listing_id}, DiscountedPrice: {body.get('discountedPrice')}")
    else:
        record("Create Discounted Food Listing", False, f"Code {code}, resp: {body}")

    # 8. Available Listings Search
    code, body, err = make_request(f"{target_base}/api/listings/available")
    if code == 200 and isinstance(body, list):
        ids = [item.get("id") for item in body]
        has_created = (free_listing_id in ids) and (discount_listing_id in ids)
        record("Public Available Listings Query", has_created, f"Retrieved {len(body)} listings, verified IDs {free_listing_id}, {discount_listing_id}")
    else:
        record("Public Available Listings Query", False, f"Code {code}, resp: {body}")

    # 9. Recipient Claims 4 Units from Free Listing
    req_id = None
    if free_listing_id and recip_token:
        claim_payload = {
            "listingId": free_listing_id,
            "quantityRequested": 4,
            "isPriority": False,
            "priorityCategory": "REGULAR",
            "notes": "Emergency meal kits for community center lunch program"
        }
        code, body, err = make_request(f"{target_base}/api/requests", method="POST", headers=recip_headers, data=claim_payload)
        if code in [200, 201] and isinstance(body, dict) and "id" in body:
            req_id = body["id"]
            record("Recipient Food Claim (Quantity: 4)", True, f"Request ID: {req_id}, Status: {body.get('status')}")
        else:
            record("Recipient Food Claim (Quantity: 4)", False, f"Code {code}, resp: {body}")

    # 10. Verify Inventory Reduction (10 -> 6 available)
    if free_listing_id:
        code, body, err = make_request(f"{target_base}/api/listings/{free_listing_id}")
        if code == 200 and isinstance(body, dict):
            avail = body.get("remainingQuantity")
            record("Inventory Real-Time Reduction", avail == 6, f"Expected 6, Actual remainingQuantity: {avail}")
        else:
            record("Inventory Real-Time Reduction", False, f"Code {code}, resp: {body}")

    # 11. Over-Allocation Prevention (Request 10 units when only 6 available)
    if free_listing_id and recip_token:
        over_claim_payload = {
            "listingId": free_listing_id,
            "quantityRequested": 10,
            "isPriority": False,
            "priorityCategory": "REGULAR"
        }
        code, body, err = make_request(f"{target_base}/api/requests", method="POST", headers=recip_headers, data=over_claim_payload)
        if code in [400, 409, 500]:
            msg = body.get("message") if isinstance(body, dict) else str(body)
            record("Inventory Over-Allocation Prevention", True, f"Code {code}: Successfully prevented over-allocation ({msg[:70]})")
        else:
            record("Inventory Over-Allocation Prevention", False, f"Code {code}: Expected rejection for over-allocation, received {body}")

    # 12. Depleted Food Handling
    if free_listing_id and recip_token:
        deplete_payload = {
            "listingId": free_listing_id,
            "quantityRequested": 6,
            "isPriority": False,
            "priorityCategory": "REGULAR"
        }
        code, body, err = make_request(f"{target_base}/api/requests", method="POST", headers=recip_headers, data=deplete_payload)
        
        # Now try to request 1 more from depleted listing
        code2, body2, err2 = make_request(f"{target_base}/api/requests", method="POST", headers=recip_headers, data={
            "listingId": free_listing_id,
            "quantityRequested": 1
        })
        if code2 in [400, 409, 500]:
            msg2 = body2.get("message") if isinstance(body2, dict) else str(body2)
            record("Depleted Inventory Rejection Handling", True, f"Code {code2}: Rejection confirmed for depleted stock ({msg2[:70]})")
        else:
            record("Depleted Inventory Rejection Handling", False, f"Code {code2}: Expected rejection for depleted listing, got {body2}")

    # 13. Expired Food Listing Error Handling
    past_time = (datetime.datetime.now() - datetime.timedelta(days=1)).strftime("%Y-%m-%dT%H:%M:%S")
    expired_payload = {
        "title": "Expired Bread",
        "category": "BAKERY",
        "quantity": 5,
        "unit": "MEALS",
        "foodType": "FREE_DONATION",
        "locationAddress": "123 Farm Way",
        "locationCity": "Metropolis",
        "expiryTime": past_time
    }
    code, body, err = make_request(f"{target_base}/api/listings", method="POST", headers=prov_headers, data=expired_payload)
    if code in [400, 422]:
        record("Expired Food Validation Handling", True, f"Code {code}: Past expiry was successfully rejected by @Future validation")
    else:
        record("Expired Food Validation Handling", False, f"Code {code}: Past expiry was improperly accepted: {body}")

    # 14. Collection Token Generation & Retrieval
    token_code = None
    if req_id and recip_token:
        code, body, err = make_request(f"{target_base}/api/tokens/request/{req_id}", headers=recip_headers)
        if code == 200 and isinstance(body, dict) and "tokenCode" in body:
            token_code = body["tokenCode"]
            record("Collection Token Generation & Retrieval", True, f"Token: {token_code}, Status: {body.get('status')}")
        else:
            record("Collection Token Generation & Retrieval", False, f"Code {code}, resp: {body}")

    # 15. Collection Token IDOR Protection
    if req_id:
        unrelated_recip_payload = {
            "fullName": "Unrelated User",
            "email": f"unrelated_{ts}@foodshare.org",
            "password": "Password123!",
            "phone": "5554443332",
            "role": "ROLE_RECIPIENT",
            "providerType": "NONE"
        }
        c_u, b_u, _ = make_request(f"{target_base}/api/auth/register", method="POST", data=unrelated_recip_payload)
        if c_u in [200, 201] and "token" in b_u:
            unrelated_headers = {"Authorization": f"Bearer {b_u['token']}"}
            code_idor, body_idor, _ = make_request(f"{target_base}/api/tokens/request/{req_id}", headers=unrelated_headers)
            if code_idor in [400, 403, 404, 500]:
                record("Collection Token IDOR Protection", True, f"Code {code_idor}: Unrelated user securely blocked from retrieving recipient's token")
            else:
                record("Collection Token IDOR Protection", False, f"Code {code_idor}: Vulnerable! Unrelated user retrieved token: {body_idor}")

    # 16. Verify Token Details (Public or Authenticated)
    if token_code:
        code, body, err = make_request(f"{target_base}/api/tokens/verify", method="POST", headers=prov_headers, data={"tokenCode": token_code})
        if code == 200 and isinstance(body, dict) and body.get("valid") is True:
            record("Token Pre-Collection Verification", True, f"Valid: {body.get('valid')}, Quantity: {body.get('quantity')}")
        else:
            record("Token Pre-Collection Verification", False, f"Code {code}, resp: {body}")

    # 17. Confirm Collection (Token Redemption & Burning)
    if token_code and prov_token:
        code, body, err = make_request(f"{target_base}/api/tokens/confirm-collection", method="POST", headers=prov_headers, data={"tokenCode": token_code})
        if code == 200 and isinstance(body, dict) and body.get("tokenStatus") in ["VERIFIED", "COLLECTED", "REDEEMED"]:
            record("Token Collection & Redemption (Burn)", True, f"TokenStatus: {body.get('tokenStatus')}")
        else:
            record("Token Collection & Redemption (Burn)", False, f"Code {code}, resp: {body}")

    # 18. Prevent Double Redemption (Single-Use Token Enforcement)
    if token_code and prov_token:
        code, body, err = make_request(f"{target_base}/api/tokens/confirm-collection", method="POST", headers=prov_headers, data={"tokenCode": token_code})
        if code in [400, 409, 500]:
            msg = body.get("message") if isinstance(body, dict) else str(body)
            record("Token Single-Use Burn Enforcement", True, f"Code {code}: Successfully prevented reuse/double redemption ({msg[:70]})")
        else:
            record("Token Single-Use Burn Enforcement", False, f"Code {code}: Failed to prevent token double redemption: {body}")

    # 19. Admin User Management API
    if admin_token:
        code, body, err = make_request(f"{target_base}/api/users", headers=admin_headers)
        if code == 200 and isinstance(body, list) and len(body) > 0:
            has_password = any("password" in u for u in body)
            record("Admin GET /api/users Retrieval", not has_password, f"Retrieved {len(body)} users safely without password hash exposure")
        else:
            record("Admin GET /api/users Retrieval", False, f"Code {code}, resp: {body}")

    # 20. Admin Role Assignment (Assign ROLE_VOLUNTEER)
    if admin_token and recipient_email:
        c, b, _ = make_request(f"{target_base}/api/users", headers=admin_headers)
        recip_user_id = None
        if c == 200 and isinstance(b, list):
            for u in b:
                if u.get("email") == recipient_email:
                    recip_user_id = u.get("id")
                    break
        if recip_user_id:
            code, body, err = make_request(f"{target_base}/api/users/{recip_user_id}/role?role=ROLE_VOLUNTEER", method="PUT", headers=admin_headers)
            if code == 200 and isinstance(body, dict) and body.get("role") == "ROLE_VOLUNTEER":
                record("Admin Role Assignment Management", True, f"Updated user {recip_user_id} role to ROLE_VOLUNTEER")
            else:
                record("Admin Role Assignment Management", False, f"Code {code}, resp: {body}")
        else:
            record("Admin Role Assignment Management", False, "Recipient user ID not found")

    # 21. Non-Admin Forbidden on Admin API
    if recip_token:
        code, body, err = make_request(f"{target_base}/api/users", headers=recip_headers)
        if code in [401, 403]:
            record("RBAC Admin Protection (403/401 for Non-Admin)", True, f"Code {code}: Non-admin user successfully blocked from admin API")
        else:
            record("RBAC Admin Protection (403/401 for Non-Admin)", False, f"Expected 401/403, got code {code}: {body}")

    # 22. Frontend Static Assets Availability
    frontend_files = [
        "/index.html", "/login.html", "/register.html", 
        "/admin-dashboard.html", "/user-dashboard.html", 
        "/provider-dashboard.html", "/volunteer-dashboard.html", 
        "/available-food.html", "/add-listing.html", "/js/api.js"
    ]
    fe_passed = True
    missing_fe = []
    for f in frontend_files:
        code, body, _ = make_request(f"{target_base}{f}")
        if code != 200:
            fe_passed = False
            missing_fe.append(f"{f} ({code})")
    record("Frontend Static Assets Delivery", fe_passed, "All 10 core frontend pages and JS assets returned HTTP 200" if fe_passed else f"Missing: {missing_fe}")

    # Summary
    total = len(results)
    passed_count = sum(1 for r in results if r["status"] == "PASS")
    print(f"\n=======================================================")
    print(f"VERIFICATION RESULTS: {passed_count}/{total} PASSED")
    print(f"=======================================================\n")
    return passed_count == total

if __name__ == "__main__":
    target = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8088"
    success = run_tests(target)
    sys.exit(0 if success else 1)
