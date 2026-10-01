using System;
using System.Net;
using System.Text;

public class CPHInline
{
    static readonly int PUERTO = 47110; // igual que en config/kickcreeper.properties

    public bool Execute()
    {
        string nombre = Arg("rawInput") ?? Arg("userInput") ?? Arg("input") ?? "";
        string usuario = Arg("user") ?? Arg("userName") ?? "";
        string url = "http://127.0.0.1:" + PUERTO + "/creeper?nombre=" + Uri.EscapeDataString(nombre)
                   + "&usuario=" + Uri.EscapeDataString(usuario);
        try
        {
            using (var wc = new WebClient()) { wc.Encoding = Encoding.UTF8; wc.DownloadString(url); }
            return true;
        }
        catch (Exception e)
        {
            CPH.LogWarn("[KickCreeper] No se pudo spawnear: " + e.Message);
            return false;
        }
    }

    string Arg(string k)
    {
        string v;
        return CPH.TryGetArg(k, out v) && !string.IsNullOrWhiteSpace(v) ? v : null;
    }
}
