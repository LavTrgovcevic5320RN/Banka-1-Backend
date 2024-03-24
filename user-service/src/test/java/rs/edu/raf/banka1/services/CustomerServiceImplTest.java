package rs.edu.raf.banka1.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import rs.edu.raf.banka1.model.*;
import rs.edu.raf.banka1.repositories.BankAccountRepository;
import rs.edu.raf.banka1.repositories.CurrencyRepository;
import rs.edu.raf.banka1.repositories.CustomerRepository;
import rs.edu.raf.banka1.repositories.UserRepository;
import rs.edu.raf.banka1.requests.InitialActivationRequest;
import rs.edu.raf.banka1.requests.createCustomerRequest.AccountData;
import rs.edu.raf.banka1.requests.createCustomerRequest.CreateCustomerRequest;
import rs.edu.raf.banka1.requests.createCustomerRequest.CustomerData;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CustomerServiceImplTest {
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerServiceImpl sut;

    private InitialActivationRequest initialActivationRequest;

    @BeforeEach
    public void setUp(){
        initialActivationRequest = new InitialActivationRequest();
        initialActivationRequest.setEmail("test@gmail.com");
        initialActivationRequest.setPhoneNumber("123456789");
        initialActivationRequest.setAccountNumber("123456789");
    }

    @Test
    public void createNewCustomerSuccessful() {
        CustomerData customerData = new CustomerData();
        customerData.setFirstName("Test");
        customerData.setLastName("Test");
        customerData.setPosition("Test");
        customerData.setDateOfBirth(123456789L);
        customerData.setGender("Test");
        customerData.setEmail("test@gmail.com");
        customerData.setPhoneNumber("123456789");
        customerData.setAddress("Test");
        customerData.setJmbg("Test");

        AccountData accountData = new AccountData();
        accountData.setAccountType(AccountType.CURRENT);
        accountData.setCurrencyName("RSD");
        accountData.setMaintenanceCost(123.0);

        CreateCustomerRequest createCustomerRequest = new CreateCustomerRequest();
        createCustomerRequest.setCustomerData(customerData);
        createCustomerRequest.setAccountData(accountData);

        when(currencyRepository.findCurrencyByCurrencyCode("RSD")).thenReturn(Optional.of(new Currency()));

        try (MockedStatic<SecurityContextHolder> securityContextHolderMockedStatic =
                     Mockito.mockStatic(SecurityContextHolder.class)) {
            Authentication authentication = mock(Authentication.class);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            securityContextHolderMockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            UserDetails userDetails = mock(UserDetails.class);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userDetails.getUsername()).thenReturn("admin@admin.com");

            User user = new User();
            user.setUserId(1L);
            when(userRepository.findByEmail("admin@admin.com")).thenReturn(Optional.of(user));

            Customer customer = new Customer();
            customer.setEmail("test@gmail.com");
            customer.setUserId(2L);
            when(customerRepository.save(any())).thenReturn(customer);

            sut.createNewCustomer(createCustomerRequest);

            verify(customerRepository).save(any());
            verify(bankAccountRepository).save(any());
            verify(emailService).sendActivationEmail(anyString(), anyString(), anyString());
        }
    }


    @Test
    public void initialActivationSuccessful() {
        Customer customer = new Customer();
        customer.setEmail("test@gmail.com");
        customer.setPhoneNumber("123456789");
        customer.setActivationToken("testactivationtoken");
        BankAccount bankAccount = new BankAccount();
        bankAccount.setCustomer(customer);
        when(bankAccountRepository.findBankAccountByAccountNumber("123456789")).thenReturn(Optional.of(bankAccount));

        boolean result = sut.initialActivation(initialActivationRequest);

        assertTrue(result);
        verify(emailService).sendActivationEmail(eq("test@gmail.com"), anyString(), anyString());
    }

    @Test
    public void initialActivationBankAccountDoesntExist(){
        when(bankAccountRepository.findBankAccountByAccountNumber("123456789")).thenReturn(Optional.empty());

        boolean result = sut.initialActivation(initialActivationRequest);

        assertFalse(result);
        verify(emailService, never()).sendActivationEmail(anyString(), anyString(), anyString());
    }

    @Test
    public void initialActivationEmailIsntCorrect(){
        Customer customer = new Customer();
        customer.setEmail("test123@gmail.com");
        customer.setPhoneNumber("123456789");
        customer.setActivationToken("testactivationtoken");
        BankAccount bankAccount = new BankAccount();
        bankAccount.setCustomer(customer);
        when(bankAccountRepository.findBankAccountByAccountNumber("123456789")).thenReturn(Optional.of(bankAccount));

        boolean result = sut.initialActivation(initialActivationRequest);

        assertFalse(result);
        verify(emailService, never()).sendActivationEmail(anyString(), anyString(), anyString());
    }

    @Test
    public void initialActivationPhoneNumberIsntCorrect(){
        Customer customer = new Customer();
        customer.setEmail("test@gmail.com");
        customer.setPhoneNumber("123456780");
        customer.setActivationToken("testactivationtoken");
        BankAccount bankAccount = new BankAccount();
        bankAccount.setCustomer(customer);
        when(bankAccountRepository.findBankAccountByAccountNumber("123456789")).thenReturn(Optional.of(bankAccount));

        boolean result = sut.initialActivation(initialActivationRequest);

        assertFalse(result);
        verify(emailService, never()).sendActivationEmail(anyString(), anyString(), anyString());
    }

    @Test
    public void activateNewCustomerSuccessful(){
        Customer customer = new Customer();
        customer.setEmail("test@gmail.com");
        customer.setPhoneNumber("123456780");
        customer.setActivationToken("testactivationtoken");
        customer.setActive(false);
        BankAccount bankAccount = new BankAccount();
        bankAccount.setCustomer(customer);
        customer.setAccountIds(List.of(bankAccount));
        when(customerRepository.findCustomerByActivationToken("testactivationtoken")).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);

        Long result = sut.activateNewCustomer("testactivationtoken", "password");

        assertTrue(customer.getActive());
        assertNull(customer.getActivationToken());

        assertEquals(bankAccount.getAccountStatus(), "ACTIVE");

        verify(customerRepository).save(customer);
        verify(bankAccountRepository).save(bankAccount);
    }

    @Test
    public void activateNewCustomerTokenDoesntExist(){
        when(customerRepository.findCustomerByActivationToken("testactivationtoken")).thenReturn(Optional.empty());

        Long result = sut.activateNewCustomer("testactivationtoken", "password");

        assertNull(result);
        verify(customerRepository, never()).save(any());
        verify(bankAccountRepository, never()).save(any());
    }
}