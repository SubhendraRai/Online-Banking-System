<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="pageTitle" value="Transfer Funds | Apex Banking System" scope="request"/>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<div class="auth-wrapper" style="min-height:auto; padding: 2rem 0;">
    <div class="auth-card" style="max-width: 580px;">

        <!-- 3-Step Wizard Navigation Stepper -->
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:2rem; border-bottom:1px solid var(--color-border); padding-bottom:1rem;">
            <div style="font-weight:700; color:var(--color-primary); border-bottom:2px solid var(--color-primary); padding-bottom:0.25rem;">
                1. Enter Details
            </div>
            <div style="color:var(--color-text-muted);">
                2. Confirmation
            </div>
            <div style="color:var(--color-text-muted);">
                3. Receipt
            </div>
        </div>

        <div class="auth-header" style="margin-bottom:1.5rem;">
            <div class="auth-icon-badge">
                &#10148;
            </div>
            <h1 class="auth-title">Instant Fund Transfer</h1>
            <p class="auth-subtitle">Send money seamlessly to any verified account holder</p>
        </div>

        <form action="<c:url value='/customer/transfer'/>" method="post" id="transferStep1Form">
            <div class="form-group">
                <label for="fromAccount" class="form-label">Debit Source Account</label>
                <select name="fromAccount" id="fromAccount" class="form-control" required>
                    <c:choose>
                        <c:when test="${not empty accounts}">
                            <c:forEach var="acc" items="${accounts}">
                                <option value="<c:out value='${acc.accountNo}'/>"
                                    ${acc.accountNo == sessionScope.PENDING_TRANSFER_DRAFT.fromAccount ? 'selected' : ''}>
                                    <c:out value="${acc.accountNo}"/> (<c:out value="${acc.accountType}"/> - Available: <c:out value="${acc.formattedAvailableBalance}"/>)
                                </option>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <option value="">No active accounts available</option>
                        </c:otherwise>
                    </c:choose>
                </select>
                <span class="form-hint">Choose your account to be debited.</span>
            </div>

            <div class="form-group">
                <label for="toAccount" class="form-label">Beneficiary Account Number</label>
                <input type="text" name="toAccount" id="toAccount" class="form-control"
                       placeholder="12-digit Account Number (e.g., 100000000002)"
                       pattern="[0-9]{12}" minlength="12" maxlength="12" required
                       value="<c:out value='${sessionScope.PENDING_TRANSFER_DRAFT.toAccount}'/>">
                <span class="form-hint">Recipient's verified name will be displayed on the next confirmation screen.</span>
            </div>

            <div class="form-group">
                <label for="amount" class="form-label">Transfer Amount (INR)</label>
                <div style="position:relative;">
                    <span style="position:absolute; left:1rem; top:50%; transform:translateY(-50%); font-weight:700; color:var(--color-text-muted);">₹</span>
                    <input type="number" name="amount" id="amount" class="form-control" style="padding-left:2.25rem;"
                           placeholder="0.00" min="0.01" step="0.01" required
                           value="<c:out value='${sessionScope.PENDING_TRANSFER_DRAFT.amount}'/>">
                </div>
                <span class="form-hint">Per-transfer limit: ₹50,000.00 &bull; Daily aggregate limit: ₹1,00,000.00</span>
            </div>

            <div class="form-group">
                <label for="remarks" class="form-label">Payment Remarks / Memo (Optional)</label>
                <input type="text" name="remarks" id="remarks" class="form-control"
                       placeholder="e.g. Rent, Tuition fees, Invoice payment" maxlength="100"
                       value="<c:out value='${sessionScope.PENDING_TRANSFER_DRAFT.remarks}'/>">
            </div>

            <div style="display:flex; gap:0.75rem; margin-top:1.5rem;">
                <a href="<c:url value='/customer/dashboard'/>" class="btn btn-secondary" style="flex:1;">
                    Cancel
                </a>
                <button type="submit" class="btn btn-primary" style="flex:2;">
                    Review & Continue &rarr;
                </button>
            </div>
        </form>
    </div>
</div>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
