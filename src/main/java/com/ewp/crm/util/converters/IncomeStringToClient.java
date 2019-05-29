package com.ewp.crm.util.converters;

import com.ewp.crm.models.Client;
import com.ewp.crm.models.SocialProfile;
import com.ewp.crm.models.SocialProfile.SocialNetworkType;
import com.ewp.crm.service.interfaces.VKService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class IncomeStringToClient {

    private final VKService vkService;
    private static final Logger logger = LoggerFactory.getLogger(IncomeStringToClient.class);
    private Environment env;

    @Autowired
    public IncomeStringToClient(VKService vkService, Environment env) {
        this.vkService = vkService;
        this.env = env;
    }

    public Client convert(String income) {
        Client client = null;
        logger.info("Start of conversion");
        if (income != null && !income.isEmpty()) {
            String workString = prepareForm(income);
            if (income.contains("Начать обучение")) {
                client = parseClientFormOne(workString);
            } else if (income.contains("Месяц в подарок")) {
                client = parseClientFormOne(workString);
            } else if (income.contains("Остались вопросы")) {
                client = parseClientFormTwo(workString);
            } else if (income.contains("Задать вопрос")) {
                client = parseClientFormThree(workString);
            } else if (income.contains("Java Test")) {
                client = parseClientFormFour(workString);
            } else if (income.contains("javalearn")) {
                client = parseClientFormJavaLearn(workString);
            } else {
                logger.error("The incoming email does not match any of the templates!!!");
            }
            vkService.fillClientFromProfileVK(client);
        }
        return client;
    }

    private static String prepareForm(String text) {
        if (text.contains("Страница")) {
            text = text.substring(text.indexOf("Страница:"), text.length());
        }
        return text.replaceAll("<b>|</b>|<p>|</p>|(\\r\\n|\\n)", "");
    }

    private Client parseClientFormOne(String form) {
        logger.info("Parsing FormOne...");
        String removeExtraCharacters = form.substring(form.indexOf("Страница"), form.length())
                .replaceAll(" ", "~")
                .replaceAll("Name~5", "Name")
                .replaceAll("Email~5", "Email")
                .replaceAll("Соц~~сеть", "Соцсеть");
        String[] createArrayFromString = removeExtraCharacters.split("<br~/>");
        Map<String, String> clientData = createMapFromClientData(createArrayFromString);
        Map<String, String> clientName = splitClientName(clientData.get("Name").replaceAll("~", " "));
        Client.Builder clientBuilder = new Client.Builder(clientName.get("firstName"),
                                                            clientData.get("Телефон").replace("~", ""),
                                                            clientData.get("Email").replace("~", ""));
        clientBuilder.lastName(clientName.get("lastName"));
        if (clientData.containsKey("Страна")) {
            clientBuilder.country(clientData.get("Страна").replace("~", ""));
        }
        if (clientData.containsKey("Город")) {
            clientBuilder.city(clientData.get("Город").replace("~", ""));
        }
        Client client = clientBuilder.build();
        client.setClientDescriptionComment(clientData.get("Форма").replace("~", " "));
        if (clientData.containsKey("Запрос")) {
            client.setRequestFrom(clientData.get("Запрос").replace("~", ""));
        }
        checkSocialNetworks(client, clientData);
        logger.info("FormOne parsing finished");
        return client;
    }

    private Client parseClientFormTwo(String form) {
        logger.info("Parsing FormTwo...");
        String removeExtraCharacters = form.substring(form.indexOf("Форма"), form.length())
                .replaceAll(" ", "~")
                .replaceAll("Name~3", "Name");
        String[] createArrayFromString = removeExtraCharacters.split("<br~/>");
        Map<String, String> clientData = createMapFromClientData(createArrayFromString);
        Map<String, String> clientName = splitClientName(clientData.get("Name").replaceAll("~", " "));
        Client.Builder clientBuilder = new Client.Builder(clientName.get("firstName"),
                                                            clientData.get("Phone").replace("~", ""),
                                                            clientData.get("Email").replace("~", ""));
        Client client = clientBuilder.lastName(clientName.get("lastName")).build();
        String question = clientData.get("Vopros");
        String formattedQuestion = question.replaceAll("~", " ");
        client.setClientDescriptionComment("Вопрос: " + formattedQuestion);
        checkSocialNetworks(client, clientData);
        logger.info("FormTwo parsing finished");
        return client;
    }

    private Client parseClientFormThree(String form) {
        logger.info("Parsing FormThree...");
        String removeExtraCharacters = form.substring(form.indexOf("Форма"), form.length())
                .replaceAll(" ", "~")
                .replaceAll("Name~3", "Name")
                .replaceAll("Phone~6", "Phone")
                .replaceAll("Email~2", "Email")
                .replaceAll("Social~2", "Social");
        String[] createArrayFromString = removeExtraCharacters.split("<br~/>");
        Map<String, String> clientData = createMapFromClientData(createArrayFromString);
        Map<String, String> clientName = splitClientName(clientData.get("Name").replaceAll("~", " "));
        Client.Builder clientBuilder = new Client.Builder(clientName.get("firstName"),
                                                            clientData.get("Phone").replace("~", ""),
                                                            clientData.get("Email").replace("~", ""));
        Client client = clientBuilder.lastName(clientName.get("lastName")).build();
        String question = clientData.get("Вопрос");
        String formattedQuestion = question.replaceAll("~", " ");
        client.setClientDescriptionComment("Вопрос: " + formattedQuestion);
        checkSocialNetworks(client, clientData);
        logger.info("Form Three parsing finished");
        return client;
    }

    private Client parseClientFormFour(String form) {
        logger.info("Parsing FormFour...");
        String removeExtraCharacters = form.substring(form.indexOf("Страница"), form.length())
                .replaceAll(" ", "~")
                .replaceAll("Email~2", "Email")
                .replaceAll("Phone~6", "Phone")
                .replaceAll("City~6", "Country");
        String[] createArrayFromString = removeExtraCharacters.split("<br~/>");
        Map<String, String> clientData = createMapFromClientData(createArrayFromString);
        Map<String, String> clientName = splitClientName(clientData.get("Name").replaceAll("~", " "));
        Client.Builder clientBuilder = new Client.Builder(clientName.get("firstName"),
                                                            clientData.get("Phone").replace("~", ""),
                                                            clientData.get("Email").replace("~", ""));
        clientBuilder.lastName(clientName.get("lastName"));
        if (clientData.containsKey("Country")) {
            clientBuilder.country(clientData.get("Country").replace("~", ""));
        }
        Client client = clientBuilder.build();
        client.setClientDescriptionComment(clientData.get("Форма").replace("~", " "));
        if (clientData.containsKey("Запрос")) {
            client.setRequestFrom(clientData.get("Запрос").replace("~", ""));
        }
        checkSocialNetworks(client, clientData);
        logger.info("Form Four parsing finished");
        return client;
    }

    private Client parseClientFormJavaLearn(String form) {
        logger.info("Parsing JavaLearnForm...");
        String removeExtraCharacters = form.substring(form.indexOf("Request"), form.length())
                .replaceAll("<a href=\"", "")
                .replaceAll("\">", "<br>")
                .replaceAll("Name: ", "Name:")
                .replaceAll(" ", "~");
        String[] createArrayFromString = removeExtraCharacters.split("<br>");
        Map<String, String> clientData = createMapFromClientData(createArrayFromString);
        Map<String, String> clientName = splitClientName(clientData.get("Name").replaceAll("~", " "));
        Client.Builder clientBuilder = new Client.Builder(clientName.get("firstName"),
                                                            clientData.get("Phone").replace("~", ""),
                                                            clientData.get("Email").replace("~", ""));
        Client client = clientBuilder.lastName(clientName.get("lastName")).build();
        client.setClientDescriptionComment(env.getProperty("messaging.client.description.java-learn-link"));
        checkSocialNetworks(client, clientData);
        logger.info("JavaLearnForm parsing finished");
        return client;
    }

    private void checkSocialNetworks(Client client, Map<String, String> clientData) {
        String link = org.apache.commons.lang.StringUtils.EMPTY;
        if (clientData.containsKey("Social")) {
            link = clientData.get("Social");
        } else if (clientData.containsKey("social")) {
            link = clientData.get("social");
        } else if (clientData.containsKey("Соцсеть")) {
            link = clientData.get("Соцсеть");
        }
        link = link.replaceAll("~", "");
        if (!link.isEmpty()) {
            SocialProfile currentSocialProfile = getSocialNetwork(link);
            if (currentSocialProfile.getSocialNetworkType().getName().equals("unknown")) {
                client.setComment(String.format(env.getProperty("messaging.client.socials.invalid-link"), link));
                logger.warn("Unknown social network '" + link + "'");
            } else {
                client.setSocialProfiles(Collections.singletonList(currentSocialProfile));
            }
        }
    }

    private SocialProfile getSocialNetwork(String link) {
        SocialProfile socialProfile = new SocialProfile();
        if (link.contains("vk.com") || link.contains("m.vk.com")) {
            String validLink = vkService.refactorAndValidateVkLink(link);
            if (validLink.equals("undefined")) {
                socialProfile.setSocialNetworkType(SocialNetworkType.UNKNOWN);
            } else {
                Optional<String> socialId = vkService.getIdFromLink(link);
                if (socialId.isPresent()) {
                    socialProfile.setSocialId(socialId.get());
                    socialProfile.setSocialNetworkType(SocialNetworkType.VK);
                }
            }
        } else if (link.contains("www.facebook.com") || link.contains("m.facebook.com")) {
            socialProfile.setSocialNetworkType(SocialNetworkType.FACEBOOK);
        } else {
            socialProfile.setSocialNetworkType(SocialNetworkType.UNKNOWN);
        }
        return socialProfile;
    }

    private Map<String, String> createMapFromClientData(String[] res) {
        Map<String, String> clientData = new HashMap<>();
        for (String re : res) {
            if (re.contains(":")) {
                String name = re.substring(0, re.indexOf(":"));
                String value = re.substring(re.indexOf(":") + 1, re.length());
                clientData.put(name, value);
            }
        }
        return clientData;
    }

    private Map<String, String> splitClientName(String fullName) {
        Map<String, String> clientNameMap = new HashMap<>();
        if (StringUtils.countOccurrencesOf(fullName, " ") == 1) {
            String[] full = fullName.split(" ");
            clientNameMap.put("firstName", full[0]);
            clientNameMap.put("lastName", full[1]);
        } else {
            clientNameMap.put("firstName", fullName);
        }
        return clientNameMap;
    }
}