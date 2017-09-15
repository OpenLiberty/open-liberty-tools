/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package wasdev.sample.nonpersistenttimer;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Timer;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/TimerServlet")
public class TimerServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @EJB
    private TimerSessionBean tbean;

    private String async = "Start";

    public TimerServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        PrintWriter out = response.getWriter();
        request.removeAttribute("timersubmit");

        String refresh = request.getParameter("refresh");
        String cancel = request.getParameter("cancel");
        String create = request.getParameter("timersubmit");
        String async = request.getParameter("async");
        boolean validInput = true;
        if (refresh != null) {

        } else if (cancel != null) {
            tbean.cancelTimer(request.getParameter("timerlist"));
        } else if (async != null) {
            processAsynch();
        } else if (create != null) {
            validInput = processTimer(request);
        }
        String timer = request.getParameter("tlist");
        if (timer != null) {
            System.out.println("timer: " + timer);
        }

        out.println("<H2 ALIGN=CENTER>Updated EJB Asynchronous Methods and Timers</H2>");
        out.println("<FONT FACE='arial,helvetica' SIZE=3>");

        createForm(out);

        if (validInput) {
            out.println("<BR>");
        } else {
            out.println("<p><font color=\"red\">Please enter unique Timer Info and an integer duration.</font></p>");
        }

        scrollDown(out);

        createScroll(out);
        out.close();

    }

    private boolean processTimer(HttpServletRequest request) {
        String duration = request.getParameter("duration");
        String msg = request.getParameter("message");
        String repeat = request.getParameter("repeat");
        if (tbean.uniqueInfo(msg)) {
            int time = 1;
            try {
                time = Integer.parseInt(duration);
            } catch (Exception e) {
                return false;
            }
            tbean.createTimer(time, msg, repeat != null);
            return true;
        }
        return false;
    }

    private void createScroll(PrintWriter out) {
        out.print("<B>Scheduled Timers: ");
        for (int i = 0; i < 35; i++) {
            out.print("&nbsp;");
        }
        out.println("Messages:</B> <BR>");
        out.println("<FORM METHOD=post>");
        out.println("<select name=\"timerlist\"  width=\"300\" style=\"width: 300px\" size=\"8\">");
        Collection<Timer> list = tbean.getTimers();
        for (Timer t : list)
        {
            if (t.getInfo() != null) {
                out.println("<option value=\"" + t.getInfo().toString() + "\" >" + t.getInfo().toString() + " : " + t.getNextTimeout() + "</option>");
            } else if (t.getNextTimeout() != null) {
                out.println("<option value=\"Automatic Timer\" >" + "Automatic Timer : " + t.getNextTimeout() + "</option>");
            }
        }
        out.println("</select>");
        out.println("<select name=\"messagelist\" id=\"messagelist\" width=\"350\" style=\"width: 350px\" size=\"8\" >");

        List<String> list2 = tbean.getMessagesSent();
        synchronized (list2) {
            for (String s : list2)
            {
                out.println("<option value=\"" + s + "\" >" + s + "</option>");
            }
        }
        out.println("</select>");

        out.println("<BR>");
        twoButtons(out);
        out.println("</FORM>");
    }

    private void createForm(PrintWriter out) {
        out.println("<FORM>");
        out.println("Timer Info: <input type=\"text\" name=\"message\" size=\"35\" value=\"\"><br>");
        out.println("Timer Duration (in seconds): <input type=\"text\" name=\"duration\" size=\"4\" value=\"\"><br>");
        out.println("<input type=\"checkbox\" name=\"repeat\" value=\"repeat\">Repeats<br>");
        out.println("<INPUT TYPE=submit NAME=timersubmit VALUE='Create Timer'>");
        //    out.println("<div style=\"float: left; width: 100px\"> ");
        out.println("<BR>");
        //   out.println("    <form id=\"thistoo\" method=\"post\">");
        out.println("     <input type=\"submit\" name = \"async\" value=\"" + async + " Asynchronous Methods\" >");
        //     out.println("    </form>");
        //   out.println("</div>");
        out.println("</FORM>");
    }

    private void scrollDown(PrintWriter out) {
        out.println("<script>");
        out.println("window.onload = scroll");
        out.println("function scroll() {");
        out.println("    var textarea = document.getElementById('messagelist');");
        out.println("   textarea.scrollTop = textarea.scrollHeight;");
        out.println("}");
        out.println("</script>");
    }

    private void twoButtons(PrintWriter out) {
        out.println("<div style=\"width:420px;\" name = \"div\">");
        out.println("<div style=\"float: left; width: 110px\" name = \"div2\"> ");
        out.println("<form id=\"thisone\" method=\"post\">");
        out.println("    <input type=\"submit\" name = \"cancel\" value=\"Cancel Timer\" >");
        out.println("</form>");
        out.println("</div>");
        out.println("<div style=\"float: right; width: 150px\"> ");
        out.println("    <form id=\"thistoo\" method=\"post\">");
        out.println("     <input type=\"submit\" name = \"refresh\" value=\"Refresh Lists\" >");
        out.println("    </form>");
        out.println("</div>");
        out.println("</div>");
    }

    private void processAsynch() {
        if (async.equals("Start")) {
            tbean.startAsyncMethods();
            tbean.printAsyncMessage();
            tbean.printAsyncMessage2();
            async = "Stop";
        } else {
            async = "Start";
            tbean.stopAsyncMethods();
        }
    }
}
