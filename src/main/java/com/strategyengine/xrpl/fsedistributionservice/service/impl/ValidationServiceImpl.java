package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.xrpl.xrpl4j.crypto.PrivateKey;
import org.xrpl.xrpl4j.keypairs.KeyPair;
import org.xrpl.xrpl4j.wallet.DefaultWalletFactory;
import org.xrpl.xrpl4j.wallet.Wallet;
import org.xrpl.xrpl4j.wallet.WalletFactory;

import com.google.common.annotations.VisibleForTesting;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrpScanClient;
import com.strategyengine.xrpl.fsedistributionservice.client.xrp.XrplClientService;
import com.strategyengine.xrpl.fsedistributionservice.entity.PaymentRequestEnt;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropRequestStatus;
import com.strategyengine.xrpl.fsedistributionservice.entity.types.DropType;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FsePaymentTrustlinesRequest;
import com.strategyengine.xrpl.fsedistributionservice.model.FseTrustLine;
import com.strategyengine.xrpl.fsedistributionservice.repo.PaymentRequestRepo;
import com.strategyengine.xrpl.fsedistributionservice.rest.exception.BadRequestException;
import com.strategyengine.xrpl.fsedistributionservice.service.ConfigService;
import com.strategyengine.xrpl.fsedistributionservice.service.CurrencyHexService;
import com.strategyengine.xrpl.fsedistributionservice.service.ValidationService;

import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class ValidationServiceImpl implements ValidationService {

	@Autowired
	protected CurrencyHexService currencyHexService;

	@Autowired
	protected XrplClientService xrplClientService;

	@Autowired
	protected ConfigService configService;

	@VisibleForTesting
	@Autowired
	protected XrpScanClient xrpScanClient;

	private Set<String> kycBypass;

	private Set<String> competitors;

	private static final String specialCharactersString = "!@#$%&*()'+,-./:;<=>?[]^_`{|} ";

	@Autowired
	protected PaymentRequestRepo paymentRequestRepo;

	@PostConstruct
	public void init() throws Exception {


		try (InputStream inputStream = getClass().getResourceAsStream("/kycbypass.txt");
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			kycBypass = reader.lines().collect(Collectors.toSet());
		}

		try (InputStream inputStream = getClass().getResourceAsStream("/unsupported.txt");
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			competitors = reader.lines().collect(Collectors.toSet());
		}
	}

	@Override
	public boolean isValidClassicAddress(String s) {
		if (s == null || s.trim().isEmpty()) {
			return false;
		}

		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if (specialCharactersString.contains(Character.toString(ch))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void validateAirdropNotAlreadyQueuedForIssuer(String issuingAddress) {

		if (StringUtils.isEmpty(issuingAddress)) {
			return;
		}
		List<PaymentRequestEnt> payments = paymentRequestRepo.findAll(Example.of(PaymentRequestEnt.builder()
				.status(DropRequestStatus.IN_PROGRESS).trustlineIssuerClassicAddress(issuingAddress).build()));
		payments.addAll(paymentRequestRepo
				.findAll(Example.of(PaymentRequestEnt.builder().status(DropRequestStatus.POPULATING_ADDRESSES)
						.trustlineIssuerClassicAddress(issuingAddress).build())));
		payments.addAll(paymentRequestRepo.findAll(Example.of(PaymentRequestEnt.builder()
				.status(DropRequestStatus.QUEUED).trustlineIssuerClassicAddress(issuingAddress).build())));

		payments.addAll(paymentRequestRepo.findAll(Example.of(PaymentRequestEnt.builder()
				.status(DropRequestStatus.PENDING_REVIEW).trustlineIssuerClassicAddress(issuingAddress).build())));
		if (!payments.isEmpty()) {
			throw new BadRequestException(
					"There is already an airdrop in progress or pending review for the issuing address :" + issuingAddress
							+ ".  Concurrent drops from the same issuer or sending address are not permitted.");

		}

	}

	@Override
	public void validateAirdropNotAlreadyQueuedForFromAddress(String fromAddress) {

		List<PaymentRequestEnt> payments = paymentRequestRepo.findAll(Example.of(PaymentRequestEnt.builder()
				.status(DropRequestStatus.IN_PROGRESS).fromClassicAddress(fromAddress).build()));
		payments.addAll(paymentRequestRepo.findAll(Example.of(PaymentRequestEnt.builder()
				.status(DropRequestStatus.POPULATING_ADDRESSES).fromClassicAddress(fromAddress).build())));
		payments.addAll(paymentRequestRepo.findAll(Example.of(
				PaymentRequestEnt.builder().status(DropRequestStatus.QUEUED).fromClassicAddress(fromAddress).build())));

		if (!payments.isEmpty()) {
			throw new BadRequestException("There is already an airdrop in progress for the from address :" + fromAddress
					+ ".  Concurrent drops from the same issuer or sending address are not permitted.");

		}

	}

	@Override
	public void validateClassicAddress(String classicAddress) {

		if (classicAddress != null && classicAddress.startsWith("r")) {
			return;
		}

		throw new BadRequestException("Expected classic address but received " + classicAddress);

	}

	@Override
	public void validateClassicAddressAccountLookup(String classicAddress, String field) {

		if (classicAddress != null && classicAddress.startsWith("r")) {
			try {
				// validate account is good
				xrplClientService.getAccountInfo(classicAddress);
			} catch (Exception e) {
				try {
					xrplClientService.getAccountInfo(classicAddress);
				} catch (Exception e2) {
					throw new BadRequestException("Failed to get " + field + ". Validate address-" + classicAddress);
				}
			}
			return;
		}

		throw new BadRequestException("Expected classic address but received " + classicAddress);

	}

	private void validateFromAddressMatchesKeys(@NonNull String fromClassicAddress, @NonNull String fromPrivateKey,
			@NonNull String fromSigningPublicKey) {

		try {
			WalletFactory walletFactory = DefaultWalletFactory.getInstance();
			Wallet wallet = walletFactory.fromKeyPair(
					KeyPair.builder().privateKey(fromPrivateKey).publicKey(fromSigningPublicKey).build(), false);

			if (!wallet.classicAddress().value().equals(fromClassicAddress)) {
				throw new BadRequestException(
						"The supplied public and or private keys do not match with the from address.");
			}
		} catch (Exception e) {
			throw new BadRequestException("Issue with keys: " + e.getMessage());

		}

	}

	@Override
	public void validate(FsePaymentRequest payment) {

		if (payment.getDestinationTag() != null) {
			try {
				Integer.parseInt(payment.getDestinationTag());
			} catch (Exception e) {
				throw new BadRequestException("Destination Tag is invalid.  Needs to be removed or set to a number");
			}
		}
		validateDecimal(payment.getMaxXrpFeePerTransaction(), "Max XRP Fee");
		validateNotCompetitor(payment.getTrustlineIssuerClassicAddress());
		validateKyc(payment.getTrustlineIssuerClassicAddress());
		validateClassicAddressAccountLookup(payment.getFromClassicAddress(), "From Address");
		validateCurrency(payment.getCurrencyName());

		if (!payment.getCurrencyName().equalsIgnoreCase("XRP")
				|| !StringUtils.isEmpty(payment.getTrustlineIssuerClassicAddress())) {
			validateClassicAddressAccountLookup(payment.getTrustlineIssuerClassicAddress(), "Issuer Address");
		}

		if (!StringUtils.isEmpty(payment.getSnapshotCurrencyName())) {
			validateCurrency(payment.getSnapshotCurrencyName());
		}
		if (!StringUtils.isEmpty(payment.getSnapshotTrustlineIssuerClassicAddress())) {
			validateClassicAddressAccountLookup(payment.getSnapshotTrustlineIssuerClassicAddress(),
					"Snapshot Issuer Address");
		}

		if(payment.getToClassicAddresses()!=null) {
			//coult be null if an NFT issuer was entered
			payment.getToClassicAddresses().stream().forEach(a -> validateClassicAddress(a));
		}
		validateXAddress(payment.getFromPrivateKey());
		validateXAddress(payment.getFromSigningPublicKey());

		validateAmount(payment.getAmount());

		validateNotFSEorSSEImposter(payment.getCurrencyName(), payment.getTrustlineIssuerClassicAddress());
		validatePrivateKey(payment.getFromPrivateKey());
		validatePublicSigning(payment.getFromSigningPublicKey());

		validateFromAddressMatchesKeys(payment.getFromClassicAddress(), payment.getFromPrivateKey(),
				payment.getFromSigningPublicKey());

		if (!StringUtils.isEmpty(payment.getNftIssuingAddress())) {
			validateClassicAddressAccountLookup(payment.getNftIssuingAddress(),
					"NFT Issuer Address");
		}

		validateToClassicAddressOrNftIssuerPopulated(payment.getToClassicAddresses(), payment.getNftIssuingAddress());
	
	}

	private void validateToClassicAddressOrNftIssuerPopulated(List<String> toClassicAddresses,
			String nftIssuingAddress) {
		if(toClassicAddresses == null || toClassicAddresses.isEmpty()) {
			if(nftIssuingAddress == null) {
				throw new BadRequestException("Either specific addresses or and NFT issuing address must be populated");
			}
		}
		
	}

	private void validateDecimal(String val, String field) {
		if (!StringUtils.isBlank(val)) {
			try {
				new BigDecimal(val);
			} catch (Exception e) {
				throw new BadRequestException("Invalid value (" + val + ") entered for " + field
						+ ".  Examples of valid formats (.05  1.05  0.09)");
			}
		}

	}

	protected void validatePublicSigning(@NonNull String publicSigningKey) {

		if (StringUtils.isEmpty(publicSigningKey)) {
			throw new BadRequestException("Private key is required");
		}

	}

	protected void validatePrivateKey(@NonNull String privateKeyStr) {

		if (StringUtils.isEmpty(privateKeyStr)) {
			throw new BadRequestException("Private key is required");
		}
		try {
			PrivateKey.fromBase16EncodedPrivateKey(privateKeyStr);
		} catch (Exception e) {
			throw new BadRequestException("Invalid private key received");
		}

	}

	private void validateCurrency(@NonNull String currencyName) {

		if (currencyName != null && !currencyName.toUpperCase().equals("STRING")
				&& currencyHexService.isAcceptedCurrency(currencyName)) {
			return;
		}
		throw new BadRequestException("Invalid currency received " + currencyName);
	}

	private void validateAmount(@NonNull String amount) {

		try {
			double amntdbl = Double.parseDouble(amount);

			if (amntdbl <= 0) {
				throw new BadRequestException("Invalid amount received " + amount);
			}
		} catch (Exception e) {
			throw new BadRequestException("Invalid amount received " + amount);
		}

	}

	private void validateXAddress(@NonNull String key) {

		if (key != null && (key.length() >= 40 || key.length() <= 100)) {
			return;
		}

		throw new BadRequestException("Key length is incorrect - received " + key);

	}

	@Override
	public void validate(FsePaymentTrustlinesRequest payment) {

		validateNotCompetitor(payment.getTrustlineIssuerClassicAddress());

		validateClassicAddressAccountLookup(payment.getFromClassicAddress(), "From Address");

		if (!"XRP".equals(payment.getCurrencyName())) {
			validateClassicAddressAccountLookup(payment.getTrustlineIssuerClassicAddress(), "Issuer Address");
		}
		validateXAddress(payment.getFromPrivateKey());
		validateXAddress(payment.getFromSigningPublicKey());

		validateAmount(payment.getAmount());
		validateCurrency(payment.getCurrencyName());
		validateKyc(payment.getTrustlineIssuerClassicAddress());
		validateNotFSEorSSEImposter(payment.getCurrencyName(), payment.getTrustlineIssuerClassicAddress());
		validateDecimal(payment.getMaxXrpFeePerTransaction(), "Max XRP Fee");

		validatePrivateKey(payment.getFromPrivateKey());
		validatePublicSigning(payment.getFromSigningPublicKey());

		validateFromAddressMatchesKeys(payment.getFromClassicAddress(), payment.getFromPrivateKey(),
				payment.getFromSigningPublicKey());
	}

	private void validateNotFSEorSSEImposter(@NonNull String currencyName,
			@NonNull String trustlineIssuerClassicAddress) {

		if (currencyName.toUpperCase().equals("SSE")
				&& !"rMDQTunsjE32sAkBDbwixpWr8TJdN5YLxu".equals(trustlineIssuerClassicAddress)) {
			throw new BadRequestException("Unsupported SSE589 ERROR - FAILURE");

		}

		if (currencyName.toUpperCase().equals("FSE")
				&& !"rs1MKY54miDtMFEGyNuPd3BLsXauFZUSrj".equals(trustlineIssuerClassicAddress)) {
			throw new BadRequestException("Unsupported FSE589 ERROR - FAILURE");

		}
	}

	private void validateNotCompetitor(@NonNull String trustlineIssuerClassicAddress) {

		if (competitors.contains(trustlineIssuerClassicAddress)) {
			throw new BadRequestException("Unsupported CX589 ERROR - CURRENCY FAILURE");
		}

	}

	@Deprecated
	@Override
	public void validateXrpBalance(BigDecimal xrpBalance, int size) {
		double fee = Double.valueOf(XrplClientService.MAX_XRP_FEE_PER_TRANSACTION);

		if (xrpBalance.doubleValue() < (size * fee)) {

			throw new BadRequestException(
					"Add XRP to the From Address to run this transaction. Balance is too low to attempt");
		}

	}

	@Override
	public void validateFseBalance(Double fseBlance, int size, DropType dropType, boolean crossCurrencyDrop) {

		double serviceFee = isVerifiedDropType(dropType)
				? configService.getDouble(AirdropVerificationServiceImpl.DEFAULT_SERVICE_FEE_PER_INTERVAL_VERFIED)
				: configService.getDouble(AirdropVerificationServiceImpl.DEFAULT_SERVICE_FEE_PER_INTERVAL_UNVERFIED);
		double serviceFeeInterval = isVerifiedDropType(dropType)
				? configService.getDouble(AirdropVerificationServiceImpl.SERVICE_FEE_INTERVAL_VERIFIED)
				: configService.getDouble(AirdropVerificationServiceImpl.SERVICE_FEE_INTERVAL_UNVERIFIED);

		if (crossCurrencyDrop) {
			serviceFee = serviceFee * 2;
		}

		double serviceFeeIntervals = size / serviceFeeInterval;

		double intervalsRoundedUp = Math.ceil(serviceFeeIntervals);

		if (fseBlance < (intervalsRoundedUp * serviceFee)) {

			throw new BadRequestException(
					"Add FSE to the From Address to run this transaction. Balance is too low to attempt");
		}

	}

	private boolean isVerifiedDropType(DropType dropType) {
		return DropType.GLOBALID == dropType || DropType.GLOBALID_SPECIFICADDRESSES == dropType;
	}

	@Override
	public void validateDistributingTokenBalance(Optional<FseTrustLine> fromAddressTrustLine, @NonNull String amount,
			int size) {

		if (fromAddressTrustLine.isEmpty()) {
			throw new BadRequestException(
					"The fromClassicAddress does not have a trustline for the currency.  If the from address does have the trustline, then validate you have entered the correct currency name and issuing address.");
		}

		if (Double.valueOf(fromAddressTrustLine.get().getBalance()) < (Double.valueOf(amount) * size)) {
			throw new BadRequestException("The fromClassicAddress does not have enough of the currency to send "
					+ amount + " to all " + size + " trustlines.  Lower the amount or add more of the tokens to "
					+ fromAddressTrustLine.get().getClassicAddress());
		}
	}

	private void validateKyc(String issuer) {

		if (kycBypass.contains(issuer) || xrpScanClient.isKycForAddress(issuer)) {
			return;
		}

		throw new BadRequestException(
				"KYC is required to run this drop.  Please go to https://xumm.community and click follow the issue token flow "
						+ "up to the KYC completed step.  You do not need to issue a new token or pay the fee, just stop after the KYC is completed and reach out to @XummCommunity on twitter to complete your KYC.");

	}

}
