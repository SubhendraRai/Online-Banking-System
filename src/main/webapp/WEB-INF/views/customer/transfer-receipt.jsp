<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="pageTitle" value="Transfer Receipt | Apex Banking System" scope="request"/>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<div class="auth-wrapper" style="min-height:auto; padding: 2rem 0;">
    <div class="auth-card" style="max-width: 580px;">

        <!-- 3-Step Wizard Navigation Stepper -->
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:2rem; border-bottom:1px solid var(--color-border); padding-bottom:1rem;">
            <div style="color:var(--color-text-muted);">
                1. Enter Details
            </div>
            <div style="color:var(--color-text-muted);">
                2. Confirmation
            </div>
            <div style="font-weight:700; color:var(--color-success); border-bottom:2px solid var(--color-success); padding-bottom:0.25rem;">
                3. Receipt
            </div>
        </div>

        <div class="auth-header" style="margin-bottom:1.5rem;">
            <div class="auth-icon-badge" style="background:#dcfce7; color:#16a34a; font-size:2rem;">
                &#10004;
            </div>
            <h1 class="auth-title">Transfer Successful!</h1>
            <p class="auth-subtitle">Funds have been credited to the recipient's account.</p>
        </div>

        <div class="card mb-3" style="background:#ffffff; border:1px dashed var(--color-border); box-shadow:none;">
            <div class="card-body" style="padding:1.5rem;">
                <div style="text-align:center; margin-bottom:1.25rem;">
                    <span style="font-size:0.85rem; color:var(--color-text-muted); text-transform:uppercase; letter-spacing:0.05em; font-weight:700;">Amount Transferred</span>
                    <div style="font-size:2.25rem; font-weight:800; color:var(--color-text-main); margin-top:0.25rem;">
                        <c:out value="${receipt.formattedAmount}"/>
                    </div>
                    <span class="badge badge-active" style="margin-top:0.5rem;">COMPLETED</span>
                </div>

                <div style="display:grid; grid-template-columns: 140px 1fr; gap:0.75rem; font-size:0.95rem; border-top:1px solid var(--color-border); padding-top:1.25rem;">
                    <span style="color:var(--color-text-muted); font-weight:600;">Transaction ID:</span>
                    <span style="font-family:monospace; font-weight:800; color:var(--color-primary-dark);">
                        #<c:out value="${receipt.txnId}"/>
                    </span>

                    <span style="color:var(--color-text-muted); font-weight:600;">Beneficiary:</span>
                    <span style="font-weight:700;">
                        <c:out value="${receipt.recipientName}"/> (<c:out value="${receipt.toAccount}"/>)
                    </span>

                    <span style="color:var(--color-text-muted); font-weight:600;">Debited Account:</span>
                    <span style="font-family:monospace;">
                        <c:out value="${receipt.fromAccount}"/>
                    </span>

                    <span style="color:var(--color-text-muted); font-weight:600;">Date & Time:</span>
                    <span>
                        <c:out value="${receipt.formattedTimestamp}"/>
                    </span>

                    <span style="color:var(--color-text-muted); font-weight:600;">Remarks:</span>
                    <span>
                        <c:out value="${receipt.remarks}"/>
                    </span>
                </div>
            </div>
        </div>

        <div style="display:flex; gap:0.75rem; flex-direction:column;">
            <a href="<c:url value='/customer/transfer'/>" class="btn btn-primary">
                + Make Another Transfer
            </a>
            <div style="display:flex; gap:0.75rem;">
                <a href="<c:url value='/customer/transactions'/>" class="btn btn-secondary" style="flex:1;">
                    View History
                </a>
                <a href="<c:url value='/customer/dashboard'/>" class="btn btn-outline" style="flex:1;">
                    Dashboard
                </a>
            </div>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
