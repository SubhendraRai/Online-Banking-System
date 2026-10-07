<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="pageTitle" value="Confirm Fund Transfer | Apex Banking System" scope="request"/>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<div class="auth-wrapper" style="min-height:auto; padding: 2rem 0;">
    <div class="auth-card" style="max-width: 580px;">

        <!-- 3-Step Wizard Navigation Stepper -->
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:2rem; border-bottom:1px solid var(--color-border); padding-bottom:1rem;">
            <div style="color:var(--color-text-muted);">
                1. Enter Details
            </div>
            <div style="font-weight:700; color:var(--color-primary); border-bottom:2px solid var(--color-primary); padding-bottom:0.25rem;">
                2. Confirmation
            </div>
            <div style="color:var(--color-text-muted);">
                3. Receipt
            </div>
        </div>

        <div class="auth-header" style="margin-bottom:1.5rem;">
            <div class="auth-icon-badge" style="background:#e0e7ff; color:#4338ca;">
                &#9878;
            </div>
            <h1 class="auth-title">Review & Confirm</h1>
            <p class="auth-subtitle">Verify recipient details before authorizing this transaction</p>
        </div>

        <div class="card mb-3" style="background:var(--color-bg); border:1px solid var(--color-border); box-shadow:none;">
            <div class="card-body" style="padding:1.25rem;">
                <div style="display:grid; grid-template-columns: 140px 1fr; gap:0.75rem; font-size:0.95rem;">
                    <span style="color:var(--color-text-muted); font-weight:600;">Recipient Name:</span>
                    <span style="font-weight:800; color:var(--color-primary-dark); font-size:1.05rem;">
                        <c:out value="${draft.recipientName}"/>
                    </span>

                    <span style="color:var(--color-text-muted); font-weight:600;">Destination Account:</span>
                    <span style="font-family:monospace; font-weight:700;">
                        <c:out value="${draft.toAccount}"/>
                    </span>

                    <span style="color:var(--color-text-muted); font-weight:600;">Debit Account:</span>
                    <span style="font-family:monospace; font-weight:700;">
                        <c:out value="${draft.fromAccount}"/>
                    </span>

                    <span style="color:var(--color-text-muted); font-weight:600;">Transfer Amount:</span>
                    <span style="font-weight:800; font-size:1.25rem; color:var(--color-accent);">
                        <c:out value="${draft.formattedAmount}"/>
                    </span>

                    <span style="color:var(--color-text-muted); font-weight:600;">Remarks / Memo:</span>
                    <span>
                        <c:out value="${draft.remarks}"/>
                    </span>
                </div>
            </div>
        </div>

        <div class="alert alert-info" style="margin-bottom:1.5rem; font-size:0.875rem;">
            &#8505; <strong>Security Notice:</strong> Please double-check the recipient name and account number. Once confirmed, funds are debited immediately and settled into the recipient account in real time.
        </div>

        <form action="<c:url value='/customer/transfer'/>" method="post" id="transferStep2Form">
            <input type="hidden" name="action" value="execute">
            <input type="hidden" name="token" value="<c:out value='${draft.token}'/>">

            <div style="display:flex; gap:0.75rem;">
                <a href="<c:url value='/customer/transfer?step=cancel'/>" class="btn btn-secondary" style="flex:1;">
                    &larr; Cancel & Edit
                </a>
                <button type="submit" class="btn btn-primary" style="flex:2;">
                    Confirm & Send Money
                </button>
            </div>
        </form>
    </div>
</div>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
