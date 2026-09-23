package com.satyam.urlshortner.url;

import com.satyam.urlshortner.auth.EmailAlreadyRegisteredException;
import com.satyam.urlshortner.auth.InsufficientScopeException;
import com.satyam.urlshortner.auth.InvalidCredentialsException;
import com.satyam.urlshortner.auth.InvalidTokenException;
import com.satyam.urlshortner.auth.NoOrganizationMembershipException;
import com.satyam.urlshortner.apikey.ApiKeyNotFoundException;
import com.satyam.urlshortner.billing.InvalidStripeWebhookSignatureException;
import com.satyam.urlshortner.billing.NoSubscriptionException;
import com.satyam.urlshortner.billing.PlanFeatureNotAvailableException;
import com.satyam.urlshortner.billing.PlanLimitExceededException;
import com.satyam.urlshortner.billing.PlanNotFoundException;
import com.satyam.urlshortner.billing.StripeNotConfiguredException;
import com.satyam.urlshortner.billing.StripePriceNotConfiguredException;
import com.satyam.urlshortner.domain.DomainAlreadyExistsException;
import com.satyam.urlshortner.domain.DomainNotFoundException;
import com.satyam.urlshortner.domain.DomainVerificationFailedException;
import com.satyam.urlshortner.org.AccessDeniedForRoleException;
import com.satyam.urlshortner.org.InvitationEmailMismatchException;
import com.satyam.urlshortner.org.InvitationExpiredException;
import com.satyam.urlshortner.org.InvitationNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(WebExchangeBindException.class)
    public ProblemDetail handleValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Invalid request");
        log.warn("Validation failed [{}]: {}", exchange.getRequest().getPath(), detail);
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation Error");
        pd.setDetail(detail);
        return pd;
    }

    @ExceptionHandler(AliasAlreadyExistsException.class)
    public ProblemDetail handleAliasConflict(AliasAlreadyExistsException ex) {
        log.warn("Alias conflict: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Alias Already Exists");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(ReservedAliasException.class)
    public ProblemDetail handleReservedAlias(ReservedAliasException ex) {
        log.warn("Reserved alias rejected: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Reserved Alias");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(ShortUrlNotFoundException.class)
    public ProblemDetail handleShortUrlNotFound(ShortUrlNotFoundException ex) {
        log.warn("Short URL not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Short URL Not Found");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ProblemDetail handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        log.warn("Registration conflict: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Email Already Registered");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler({InvalidCredentialsException.class, InvalidTokenException.class})
    public ProblemDetail handleAuthFailure(RuntimeException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setTitle("Authentication Failed");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler({NoOrganizationMembershipException.class, AccessDeniedForRoleException.class, InvitationEmailMismatchException.class, InsufficientScopeException.class})
    public ProblemDetail handleForbidden(RuntimeException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setTitle("Access Denied");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(InvitationNotFoundException.class)
    public ProblemDetail handleInvitationNotFound(InvitationNotFoundException ex) {
        log.warn("Invitation not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Invitation Not Found");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(InvitationExpiredException.class)
    public ProblemDetail handleInvitationExpired(InvitationExpiredException ex) {
        log.warn("Invitation expired: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.GONE);
        pd.setTitle("Invitation Expired");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(FolderNotFoundException.class)
    public ProblemDetail handleFolderNotFound(FolderNotFoundException ex) {
        log.warn("Folder not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Folder Not Found");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(InvalidBulkActionException.class)
    public ProblemDetail handleInvalidBulkAction(InvalidBulkActionException ex) {
        log.warn("Invalid bulk action: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Invalid Bulk Action");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(DomainNotFoundException.class)
    public ProblemDetail handleDomainNotFound(DomainNotFoundException ex) {
        log.warn("Domain not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Domain Not Found");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(DomainAlreadyExistsException.class)
    public ProblemDetail handleDomainAlreadyExists(DomainAlreadyExistsException ex) {
        log.warn("Domain conflict: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Domain Already Registered");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(DomainVerificationFailedException.class)
    public ProblemDetail handleDomainVerificationFailed(DomainVerificationFailedException ex) {
        log.warn("Domain verification failed: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        pd.setTitle("Domain Verification Failed");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(ApiKeyNotFoundException.class)
    public ProblemDetail handleApiKeyNotFound(ApiKeyNotFoundException ex) {
        log.warn("API key not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("API Key Not Found");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(WrongPasswordException.class)
    public ProblemDetail handleWrongPassword(WrongPasswordException ex) {
        log.warn("Unlock failed: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setTitle("Incorrect Password");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler({PlanLimitExceededException.class, PlanFeatureNotAvailableException.class})
    public ProblemDetail handlePlanLimitExceeded(RuntimeException ex) {
        log.warn("Plan limit exceeded: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.PAYMENT_REQUIRED);
        pd.setTitle("Plan Limit Reached");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler({NoSubscriptionException.class, PlanNotFoundException.class})
    public ProblemDetail handleBillingNotFound(RuntimeException ex) {
        log.warn("Billing resource not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Not Found");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler({StripeNotConfiguredException.class, StripePriceNotConfiguredException.class})
    public ProblemDetail handleStripeNotConfigured(RuntimeException ex) {
        log.warn("Stripe not configured: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_IMPLEMENTED);
        pd.setTitle("Billing Not Configured");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(InvalidStripeWebhookSignatureException.class)
    public ProblemDetail handleInvalidStripeSignature(InvalidStripeWebhookSignatureException ex) {
        log.warn("Invalid Stripe webhook signature: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Invalid Webhook Signature");
        pd.setDetail(ex.getMessage());
        return pd;
    }
}
