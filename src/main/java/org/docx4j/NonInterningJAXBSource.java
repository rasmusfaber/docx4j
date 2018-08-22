package org.docx4j;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLFilterImpl;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.sax.SAXSource;

final class NonInterningJAXBSource extends SAXSource {

    /**
     * Creates a new {@link javax.xml.transform.Source} for the given content object.
     *
     * @param context       JAXBContext that was used to create
     *                      <code>contentObject</code>. This context is used
     *                      to create a new instance of marshaller and must not be null.
     * @param contentObject An instance of a JAXB-generated class, which will be
     *                      used as a {@link javax.xml.transform.Source} (by marshalling it into XML).  It must
     *                      not be null.
     * @throws javax.xml.bind.JAXBException if an error is encountered while creating the
     *                       JAXBSource or if either of the parameters are null.
     */
    public NonInterningJAXBSource(JAXBContext context, Object contentObject)
            throws JAXBException {

        this(

                        context.createMarshaller(),


                        contentObject);
    }

    /**
     * Creates a new {@link javax.xml.transform.Source} for the given content object.
     *
     * @param marshaller    A marshaller instance that will be used to marshal
     *                      <code>contentObject</code> into XML. This must be
     *                      created from a JAXBContext that was used to build
     *                      <code>contentObject</code> and must not be null.
     * @param contentObject An instance of a JAXB-generated class, which will be
     *                      used as a {@link javax.xml.transform.Source} (by marshalling it into XML).  It must
     *                      not be null.
     * @throws javax.xml.bind.JAXBException if an error is encountered while creating the
     *                       JAXBSource or if either of the parameters are null.
     */
    public NonInterningJAXBSource(Marshaller marshaller, Object contentObject)
            throws JAXBException {

        this.marshaller = marshaller;
        this.contentObject = contentObject;

        super.setXMLReader(pseudoParser);
        // pass a dummy InputSource. We don't care
        super.setInputSource(new InputSource());
    }

    private final Marshaller marshaller;
    private final Object contentObject;

    // this object will pretend as an XMLReader.
    // no matter what parameter is specified to the parse method,
    // it just parse the contentObject.
    private final XMLReader pseudoParser = new XMLReader() {
        public boolean getFeature(String name) throws SAXNotRecognizedException {
            if (name.equals("http://xml.org/sax/features/namespaces"))
                return true;
            if (name.equals("http://xml.org/sax/features/namespace-prefixes"))
                return false;
            if (name.equals("http://xml.org/sax/features/string-interning"))
                return true;
            throw new SAXNotRecognizedException(name);
        }

        public void setFeature(String name, boolean value) throws SAXNotRecognizedException {
            if (name.equals("http://xml.org/sax/features/namespaces") && value)
                return;
            if (name.equals("http://xml.org/sax/features/namespace-prefixes") && !value)
                return;
            throw new SAXNotRecognizedException(name);
        }

        public Object getProperty(String name) throws SAXNotRecognizedException {
            if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
                return lexicalHandler;
            }
            throw new SAXNotRecognizedException(name);
        }

        public void setProperty(String name, Object value) throws SAXNotRecognizedException {
            if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
                this.lexicalHandler = (LexicalHandler) value;
                return;
            }
            throw new SAXNotRecognizedException(name);
        }

        private LexicalHandler lexicalHandler;

        // we will store this value but never use it by ourselves.
        private EntityResolver entityResolver;

        public void setEntityResolver(EntityResolver resolver) {
            this.entityResolver = resolver;
        }

        public EntityResolver getEntityResolver() {
            return entityResolver;
        }

        private DTDHandler dtdHandler;

        public void setDTDHandler(DTDHandler handler) {
            this.dtdHandler = handler;
        }

        public DTDHandler getDTDHandler() {
            return dtdHandler;
        }

        // SAX allows ContentHandler to be changed during the parsing,
        // but JAXB doesn't. So this repeater will sit between those
        // two components.
        private XMLFilter repeater = new XMLFilterImpl();

        public void setContentHandler(ContentHandler handler) {
            repeater.setContentHandler(handler);
        }

        public ContentHandler getContentHandler() {
            return repeater.getContentHandler();
        }

        private ErrorHandler errorHandler;

        public void setErrorHandler(ErrorHandler handler) {
            this.errorHandler = handler;
        }

        public ErrorHandler getErrorHandler() {
            return errorHandler;
        }

        public void parse(InputSource input) throws SAXException {
            parse();
        }

        public void parse(String systemId) throws SAXException {
            parse();
        }

        public void parse() throws SAXException {
            // parses a content object by using the given marshaller
            // SAX events will be sent to the repeater, and the repeater
            // will further forward it to an appropriate component.
            try {
                marshaller.marshal(contentObject, (XMLFilterImpl) repeater);
            } catch (JAXBException e) {
                // wrap it to a SAXException
                SAXParseException se =
                        new SAXParseException(e.getMessage(),
                                null, null, -1, -1, e);

                // if the consumer sets an error handler, it is our responsibility
                // to notify it.
                if (errorHandler != null)
                    errorHandler.fatalError(se);

                // this is a fatal error. Even if the error handler
                // returns, we will abort anyway.
                throw se;
            }
        }
    };

    /**
     * Hook to throw exception from the middle of a contructor chained call
     * to this
     */
    private static Marshaller assertionFailed(String message)
            throws JAXBException {

        throw new JAXBException(message);
    }
}
