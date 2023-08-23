package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.entity.DropRecipientEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.PaymentRequestEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRecipientStatus;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRequestStatus;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropType;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentResult;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.RetryPaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.repo.DropRecipientRepo;
import com.strategyengine.xrpl.fsedistributionservice.repo.PaymentRequestRepo;
import com.strategyengine.xrpl.fsedistributionservice.rest.exception.BadRequestException;
import com.strategyengine.xrpl.fsedistributionservice.service.RetryFailedPaymentsService;
import com.strategyengine.xrpl.fsedistributionservice.service.XrplService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class RetryFailedPaymentsServiceImpl implements RetryFailedPaymentsService {

	@VisibleForTesting
	@Autowired
	protected DropRecipientRepo dropRecipientRepo;

	@VisibleForTesting
	@Autowired
	protected PaymentRequestRepo paymentRequestRepo;

	@VisibleForTesting
	@Autowired
	protected XrplService xrplService;

	@Override
	public FsePaymentResult retryFailedPayments(RetryPaymentRequest retryPaymentRequest) {

		try {
			Optional<PaymentRequestEnt> paymentOpt = paymentRequestRepo
					.findById(retryPaymentRequest.getDropRequestId());

			if (paymentOpt.isEmpty()) {
				throw new BadRequestException(
						"Could not find a previous airdrop for Airdrop ID " + retryPaymentRequest.getDropRequestId());
			}

			PaymentRequestEnt p = paymentOpt.get();

			if (DropRequestStatus.REJECTED == p.getStatus()) {
				throw new BadRequestException(
						"Airdrops that have been rejected are not eligible for retry processing.");
			}

			if (DropRequestStatus.COMPLETE != p.getStatus()) {
				throw new BadRequestException(
						"Airdrops that have not completed are not eligible for retry processing yet.   Try after the airdrop completes.");
			}

			List<DropRecipientEnt> failedPayments = dropRecipientRepo.findAll(Example.of(DropRecipientEnt.builder()
					.status(DropRecipientStatus.FAILED).dropRequestId(retryPaymentRequest.getDropRequestId()).build()));
			
			log.info("Failed payments to retry " + failedPayments.size() + " DropRequest " + retryPaymentRequest.getDropRequestId());

			if (failedPayments.size() == 1 && failedPayments.get(0).getAddress().equals(p.getFromClassicAddress())) {
				throw new BadRequestException(
						"The only payment that failed is when it tried to pay the from address.  No need to retry a payment to the from address since it is not allowed.");
			}

			if (failedPayments == null || failedPayments.isEmpty()) {
				throw new BadRequestException("Could not find any failed payments to retry for Airdrop ID "
						+ retryPaymentRequest.getDropRequestId());
			}

			final PaymentRequestEnt retryPayment;
			if (DropType.SPECIFICADDRESSES == p.getDropType()
					|| DropType.GLOBALID_SPECIFICADDRESSES == p.getDropType()) {
				retryPayment = xrplService
						.sendFsePayment(FsePaymentRequest.builder().agreeFee(true).amount(p.getAmount())
								.memo(p.getMemo())
								.currencyName(p.getCurrencyName()).fromClassicAddress(p.getFromClassicAddress())
								.fromPrivateKey(retryPaymentRequest.getFromPrivateKey())
								.fromSigningPublicKey(retryPaymentRequest.getFromSigningPublicKey())
								.globalIdVerified(DropType.GLOBALID_SPECIFICADDRESSES == p.getDropType() ? true : false)
								.maxXrpFeePerTransaction(p.getMaxXrpFeePerTransaction())
								.paymentType(p.getPaymentType())
								.snapshotCurrencyName(p.getSnapshotCurrencyName())
								.snapshotTrustlineIssuerClassicAddress(p.getSnapshotTrustlineIssuerClassicAddress())
								.useBlacklist(p.getUseBlacklist())
								.retryOfId(p.getId())
								.toClassicAddresses(
										failedPayments.stream().map(f -> f.getAddress()).collect(Collectors.toList()))
								.trustlineIssuerClassicAddress(p.getTrustlineIssuerClassicAddress()).build());

			} else {
				retryPayment = xrplService.sendFsePaymentToTrustlines(
						FsePaymentTrustlinesRequest.builder().agreeFee(true).amount(p.getAmount())
								.memo(p.getMemo())
								.currencyName(p.getCurrencyName()).fromClassicAddress(p.getFromClassicAddress())
								.fromPrivateKey(retryPaymentRequest.getFromPrivateKey())
								.fromSigningPublicKey(retryPaymentRequest.getFromSigningPublicKey())
								.globalIdVerified(DropType.GLOBALID == p.getDropType() ? true : false)
								.maxBalance(safeDouble(p.getMaxBalance())).maximumTrustlines(p.getMaximumTrustlines())
								.maxXrpFeePerTransaction(p.getMaxXrpFeePerTransaction())
								.retryOfId(p.getId())
								.minBalance(safeDouble(p.getMinBalance())).newTrustlinesOnly(p.getNewTrustlinesOnly())
								.useBlacklist(p.getUseBlacklist())
								.paymentType(p.getPaymentType())
								.snapshotCurrencyName(p.getSnapshotCurrencyName())
								.snapshotTrustlineIssuerClassicAddress(p.getSnapshotTrustlineIssuerClassicAddress())
								.trustlineIssuerClassicAddress(p.getTrustlineIssuerClassicAddress()).build(), failedPayments);
			}

		
			return FsePaymentResult.builder().id(retryPayment.getId()).build();
			
		
		} catch (BadRequestException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error trying to retry payments " + retryPaymentRequest, e);
			throw new BadRequestException("Failed to retry payment for this airdrop.  Support has been notified.");
		}

	}

	private Double safeDouble(String d) {
		if (!StringUtils.hasLength(d)) {
			return null;
		}

		try {
			return Double.parseDouble(d);
		} catch (Exception e) {
			log.error("Invalid double " + d, e);
		}
		return null;
	}

	protected Date now() {
		return new Date();
	}

}
