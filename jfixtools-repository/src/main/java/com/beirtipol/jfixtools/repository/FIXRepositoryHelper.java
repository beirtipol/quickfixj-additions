/*
 * Copyright (C) 2020  https://github.com/beirtipol
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.beirtipol.jfixtools.repository;

import fixrepository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class FIXRepositoryHelper {

    private FixRepository repository;
    private Phrases       phrases;

    @Autowired
    private FixRepositoryJAXB jaxb;

    @Autowired
    private FIXRepositoryConfig config;

    @PostConstruct
    private void initialize() {
        try {
            log.info("Loading Repository XML");
            long start = System.nanoTime();
            repository = jaxb.loadXML(config.getRepositoryFilePath());
            int nanos = 1000000;
            log.info("Loaded Repository XML in " + (System.nanoTime() - start) / nanos + "ms");
            log.info("Loading Phrases XML");
            start   = System.nanoTime();
            phrases = jaxb.loadXML(config.getPhrasesFilePath());
            log.info("Loaded Phrases XML in " + (System.nanoTime() - start) / nanos + "ms");
            log.info("Loaded JAXB Objects");
        } catch (JAXBException e) {
            log.error("Could not create JAXBContext", e);
        }
    }

    private Fields getFields() {
        List<Fix> fixes = repository.getFix();
        Fix fix = fixes.get(0);
        return fix.getFields();
    }

    private Messages getMessages() {
        List<Fix> fixes = repository.getFix();
        Fix fix = fixes.get(0);
        return fix.getMessages();
    }

    public Optional<Field> loadFieldInfo(int tag) {
        return getFields().getField().stream()
                .filter(f -> f.getId().intValue() == tag)
                .findFirst();
    }

    public Optional<Field> loadFieldInfo(String fieldName) {
        return getFields().getField().stream()
                .filter(f -> f.getName().equals(fieldName) || f.getAbbrName().equals(fieldName))
                .findFirst();
    }

    public Optional<Message> loadMessageInfo(String msgType) {
        return getMessages().getMessage().stream()
                .filter(m -> Objects.equals(m.getMsgType(), msgType))
                .findFirst();
    }


    public Optional<String> getText(String textID, PurposeT purpose) {
        return getPhrase(textID)
                .flatMap(phrase -> getText(phrase, purpose)
                        .flatMap(this::getText));
    }

    private Optional<String> getText(Text text) {
        return Optional.of(text.getContent().stream()//
                .filter(e -> e instanceof Element)//
                .map(e -> {
                    StringBuilder sb = new StringBuilder();
                    NodeList nl = ((Element) e).getChildNodes();
                    for (int i = 0; i < nl.getLength(); i++) {
                        sb.append(((org.w3c.dom.Text) nl.item(i)).getWholeText());
                    }
                    return sb.toString();
                }).collect(Collectors.joining("\n")));
    }

    private Optional<Text> getText(Phrase phrase, PurposeT purpose) {
        return phrase.getText()//
                .stream()//
                .filter(t -> t.getPurpose() == purpose)//
                .findFirst();
    }

    private Optional<Phrase> getPhrase(String textID) {
        return phrases.getPhrase()//
                .stream()//
                .filter(e -> e.getTextId().equals(textID))//
                .findFirst();
    }
}
