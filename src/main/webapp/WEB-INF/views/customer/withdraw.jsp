<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="pageTitle" value="Withdraw Funds | Apex Banking System" scope="request"/>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<div class="auth-wrapper" style="min-height:auto; padding: 2rem 0;">
    <div class="auth-card" style="max-width: 520px;">
        <div class="auth-header">
            <div class="auth-icon-badge" style="background:#fee2e2; color:#dc2626;">
                &#10514;
            </div>
            <h1 class="auth-title">Withdraw Funds</h1>
            <p class="auth-subtitle">Debit cash from your available account balance</p>
        </div>

        <form action="<c:url value='/customer/withdraw'/>" method="post" id="withdrawForm">
            <div class="form-group">
                <label for="accountNo" class="form-label">Source Account</label>
                <select name="accountNo" id="accountNo" class="form-control" required>
                    <c:choose>
                        <c:when test="${not empty accounts}">
                            <c:forEach var="acc" items="${accounts}">
                                <option value="<c:out value='${acc.accountNo}'/>">
                                    <c:out value="${acc.accountNo}"/> (<c:out value="${acc.accountType}"/> - Available: <c:out value="${acc.formattedAvailableBalance}"/>)
                                </option>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <option value="">No accounts available</option>
                        </c:otherwise>
                    </c:choose>
                </select>
                <span class="form-hint">Withdrawals adhere to minimum balance or approved overdraft limits.</span>
            </div>

            <div class="form-group">
                <label for="amount" class="form-label">Withdrawal Amount (INR)</label>
                <div style="position:relative;">
                    <span style="position:absolute; left:1rem; top:50%; transform:translateY(-50%); font-weight:700; color:var(--color-text-muted);">₹</span>
                    <input type="number" name="amount" id="amount" class="form-control" style="padding-left:2.25rem;"
                           placeholder="0.00" min="0.01" step="0.01" required>
                </div>
                <span class="form-hint">Amount must not exceed your available account balance.</span>
            </div>

            <div class="form-group">
                <label for="remarks" class="form-label">Withdrawal Remarks / Purpose (Optional)</label>
                <input type="text" name="remarks" id="remarks" class="form-control"
                       placeholder="e.g. ATM cash withdrawal, Personal expense" maxlength="100">
            </div>

            <div style="display:flex; gap:0.75rem; margin-top:1.5rem;">
                <a href="<c:url value='/customer/dashboard'/>" class="btn btn-secondary" style="flex:1;">
                    Cancel
                </a>
                <button type="submit" class="btn btn-primary" style="flex:2; background:#dc2626; border-color:#dc2626;">
                    - Confirm Withdrawal
                </button>
            </div>
        </form>
    </div>
</div>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
