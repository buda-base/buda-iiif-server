package de.digitalcollections.iiif.myhymir;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.bdrc.auth.Access;
import io.bdrc.auth.AuthProps;
import io.bdrc.auth.TokenValidation;
import io.bdrc.auth.UserProfile;
import io.bdrc.auth.model.Endpoint;
import io.bdrc.auth.rdf.RdfAuthModel;


@Component
@Order(1)
public class IIIFRdfAuthFilter implements Filter{
    
    public final static Logger log=LoggerFactory.getLogger(IIIFRdfAuthFilter.class.getName());

    @Override
    public void destroy() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        
        String token=getToken(((HttpServletRequest)req).getHeader("Authorization"));
        TokenValidation validation=null;
        UserProfile prof=null;
        if(token !=null) {
            //User is logged on
            //Getting his profile
            validation=new TokenValidation(token);
            prof=validation.getUser();
            req.setAttribute("access", new Access(prof,new Endpoint()));
        }else {
            req.setAttribute("access", new Access());
        }
        chain.doFilter(req, res);
        // TODO Auto-generated method stub
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // TODO Auto-generated method stub
        
    }
    
    String getToken(String header) {
        try {
            if(header!=null) {
                return header.split(" ")[1];
            }
        }
        catch(Exception ex) {
            ex.printStackTrace();
            log.error(ex.getMessage());
            return null;
        }
        return null;
    }

}
