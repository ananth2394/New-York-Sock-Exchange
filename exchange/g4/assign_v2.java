public string Classify(Color c)
{
    float hue = c.GetHue();
    float sat = c.GetSaturation();
    float lgt = c.GetLightness();

    if (lgt < 0.2)  return "Blacks";
    if (lgt > 0.8)  return "Whites";

    if (sat < 0.25) return "Grays";

    if (hue < 30)   return "Reds";
    if (hue < 90)   return "Yellows";
    if (hue < 150)  return "Greens";
    if (hue < 210)  return "Cyans";
    if (hue < 270)  return "Blues";
    if (hue < 330)  return "Magentas";
    return "Reds";
}
