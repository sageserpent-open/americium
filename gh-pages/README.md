# Americium Documentation Site - Jekyll Setup

Complete Jekyll documentation site for Americium with Just the Docs theme.

## Quick Start
```bash
# Clone or navigate to the repository's Jekyll sources.
cd americium/gh-pages

# Install dependencies
bundle install

# Serve locally
bundle exec jekyll serve

# Open in browser
open http://localhost:4000/americium
```

## What's Included

- ✅ **Full-text search** powered by Just the Docs
- ✅ **Responsive design** for mobile and desktop
- ✅ **Syntax highlighting** for Java, Scala, and more


## Site Structure
```
docs/
├── getting-started/
├── core-concepts/ 
├── junit5/
├── techniques/
└── reference/
```

## Deployment

### GitHub Pages

1. Push to GitHub
2. Settings → Pages → Enable
3. Source: `GitHub Actions`
4. Ensure that `Enforce HTTPS` is enabled.
5. Done! Site live at `https://your-username.github.io/americium`

### Custom Domain

Add `CNAME` file to root with your domain, or configure in GitHub Pages settings.

## Customization

Edit `_config.yml` to customize:
- Site title and description
- Base URL and repository
- Color scheme
- Navigation settings

## Contributing

All documentation pages are in Markdown format under `docs/`. To add content:

1. Create `.md` file in appropriate section
2. Add Jekyll front matter with `title`, `parent`, `nav_order`
3. Write content in Markdown
4. Test locally before committing

## Support

- **Americium Repo:** https://github.com/sageserpent-open/americium
- **Issues:** https://github.com/sageserpent-open/americium/issues
- **Theme Docs:** https://just-the-docs.github.io/just-the-docs/

## License

Documentation: MIT License  
Theme: MIT License (Just the Docs)